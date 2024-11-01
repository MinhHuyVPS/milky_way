package milky_way.service;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.transaction.Transactional;
import milky_way.dto.request.UserRequest;
import milky_way.dto.request.UserRequestLogin;
import milky_way.dto.response.*;
import milky_way.entity.Role;
import milky_way.entity.Session;
import milky_way.entity.User;
import milky_way.exception.AppException;
import milky_way.exception.ErrorCode;
import milky_way.mapper.UserMapper;
import milky_way.repository.RoleRepository;
import milky_way.repository.UserRepository;
import milky_way.utils.JwtTokenProvider;
import milky_way.utils.Notification;
import milky_way.utils.UserRole;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.beans.factory.annotation.Value;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class AuthService {
    @Value("${spring.security.oauth2.client.registration.google.client-id}")
    private String clientId;

    @Value("${spring.security.oauth2.client.registration.google.redirect.uri}")
    private String redirectUri;

    @Value("${spring.security.oauth2.client.registration.google.client-secret}")
    private String clientSecret;

    @Value("${spring.security.oauth2.client.provider.google.token-uri}")
    private String tokenUri;

    @Value("${spring.security.oauth2.client.provider.google.user-info-uri}")
    private String userInfoUri;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private SessionService sessionService;

    @Autowired
    private TokenBlacklistService tokenBlacklistService;

    @Autowired
    private RestTemplate restTemplate;

    public UserResponse registerUser(UserRequest userRequest) {
        if (userRepository.existsByEmail(userRequest.getEmail())) {
            throw new AppException(ErrorCode.EMAIL_EXIST);
        }
        if(userRepository.existsByGoogleEmail(userRequest.getEmail())){
            throw new AppException(ErrorCode.EMAIL_EXIST);
        }
        User user = userMapper.toUser(userRequest);
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        Optional<Role> userRole = roleRepository.findByName(UserRole.USER.getValue());
        if (userRole.isEmpty()) {
            Role newRole = Role.builder()
                    .name(UserRole.USER.getValue())
                    .description(Notification.REGISTER.getValue())
                    .build();
            roleRepository.save(newRole);
            userRole = Optional.of(newRole);
        }
        user.setRoles(Collections.singleton(userRole.get()));
        User saveUser = userRepository.save(user);
        return userMapper.toUserResponse(saveUser);
    }

    public Optional<AuthResponse> loginUser(UserRequestLogin userRequest) {
        Optional<User> user = Optional.ofNullable(userRepository.findByEmail(userRequest.getEmail())
                .orElseThrow(() -> new AppException(ErrorCode.EMAIL_NOT_EXIST)));
        if (!passwordEncoder.matches(userRequest.getPassword(), user.get().getPassword())) {
            throw new AppException(ErrorCode.PASSWORD_INCORRECT);
        }
        Set<Role> roles = user.get().getRoles();
        String accessToken = jwtTokenProvider.generateToken(user.get().getEmail(), roles);
        String refreshToken = jwtTokenProvider.generateRefreshToken(user.get().getEmail(), roles);
        Session session = sessionService.createSession(user.get());
        AuthResponse authResponse = new AuthResponse();
        authResponse.setAccessToken(accessToken);
        authResponse.setRefreshToken(refreshToken);
//        authResponse.setGoogleId(user.get().getGoogleId());
        authResponse.setSessionResponse(new SessionResponse(session.getId(), session.getCreatedAt(), session.getExpiresAt()));
        return Optional.of(authResponse);
    }

    public AuthLoginResponse refreshToken(String refreshToken) {
        if (!jwtTokenProvider.validateToken(refreshToken) || jwtTokenProvider.isTokenExpired(refreshToken)) {
            throw new AppException(ErrorCode.INVALID_REFRESH_TOKEN);
        }
        String email = jwtTokenProvider.getEmailFromJWT(refreshToken);
        Optional<User> user = userRepository.findByEmail(email);
        if (user.isEmpty()) {
            throw new AppException(ErrorCode.EMAIL_NOT_EXIST);
        }
        AuthLoginResponse authResponse = new AuthLoginResponse();
        Set<Role> roles = user.get().getRoles();
        String newAccessToken = jwtTokenProvider.generateToken(email, roles);
        authResponse.setAccessToken(newAccessToken);
        authResponse.setRefreshToken(refreshToken);
        Optional<Session> currentSession = sessionService.getSession(email);
        if (currentSession.isPresent()) {
            Session session = currentSession.get();
            authResponse.setSessionResponse(new SessionResponse(session.getId(), session.getCreatedAt(), session.getExpiresAt()));
        } else {
            Session newSession = sessionService.createSession(user.get());
            authResponse.setSessionResponse(new SessionResponse(newSession.getId(), newSession.getCreatedAt(), newSession.getExpiresAt()));
        }
        if (authResponse.getSessionResponse() == null) {
            throw new AppException(ErrorCode.INVALID_RESPONSE_CONTENT);
        }
        return authResponse;
    }

    public void logout(HttpServletRequest request) {
        String token = tokenBlacklistService.getJwtFromRequest(request);
        if (token != null) {
            LocalDateTime tokenExpiryDate = jwtTokenProvider.getExpiryDateFromToken(token);
            tokenBlacklistService.blacklistToken(token, tokenExpiryDate);
            String email = jwtTokenProvider.getEmailFromJWT(token);
            sessionService.invalidateSession(email);
        } else {
            throw new AppException(ErrorCode.TOKEN_NOT_FOUND);
        }
    }

    public UserProfileResponse getProfile(HttpServletRequest request) {
        String token = tokenBlacklistService.getJwtFromRequest(request);
        if (token == null || !jwtTokenProvider.validateToken(token)) {
            throw new AppException(ErrorCode.INVALID_TOKEN);
        }
        String email = jwtTokenProvider.getEmailFromJWT(token);
        if (!sessionService.isSessionValid(email)) {
            throw new AppException(ErrorCode.SESSION_INVALID);
        }
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(ErrorCode.EMAIL_NOT_EXIST));
        if (!user.getUsername().equals(email)) {
            throw new AppException(ErrorCode.UNAUTHORIZED_ACCESS);
        }
        return new UserProfileResponse(user.getUsername(), user.getEmail(), user.getFirstname(), user.getLastname(), user.getPhone(), user.getAddress(), user.getBirthday());
    }

    public AuthResponse loginWithGoogle(String googleEmail, String googleId) {
        return handleOAuth2GoogleLogin(googleEmail, googleId);
    }

    public AuthResponse loginWithFacebook(String email, String facebookId) {
        return handleOAuth2FaceBookLogin(email, facebookId);
    }

    private AuthResponse handleOAuth2GoogleLogin(String googleEmail, String googleId) {
        User user = findOrGoogleCreateUser(googleEmail, googleId);
        Session newSession = sessionService.createSession(user);
        Set<Role> roles = user.getRoles();
        String newAccessToken = jwtTokenProvider.generateToken(googleEmail, roles);
        String newRefreshToken = jwtTokenProvider.generateRefreshToken(googleEmail, roles);
        AuthResponse authResponse = new AuthResponse();
        authResponse.setAccessToken(newAccessToken);
        authResponse.setRefreshToken(newRefreshToken);
        authResponse.setSessionResponse(new SessionResponse(newSession.getId(), newSession.getCreatedAt(), newSession.getExpiresAt()));
        authResponse.setGoogleId(googleId);
        return authResponse;
    }

    private User findOrGoogleCreateUser(String googleEmail, String googleId) {
        Optional<User> existingUser = userRepository.findByGoogleEmail(googleEmail);

        if (existingUser.isPresent()) {
            User user = existingUser.get();
            linkGoogleOAuthProviderIfNecessary(user, googleId);
            return user;
        } else {
            return createGoogleUserWithOAuth(googleEmail, googleId);
        }
    }

    private void linkGoogleOAuthProviderIfNecessary(User user, String googleId) {
        String encryptedGoogleId = encryptGoogleId(googleId);
        if (user.getGoogleId() == null && googleId != null) {
            user.setGoogleId(encryptedGoogleId);
            userRepository.save(user);
        } else if (user.getGoogleId() != null && !user.getGoogleId().equals(googleId)) {
            throw new AppException(ErrorCode.LINKED_ACCOUNT_MISMATCH);
        }
    }

    private User createGoogleUserWithOAuth(String googleEmail, String googleId) {
        User user = new User();
        user.setGoogleEmail(googleEmail);
        if (googleId != null) user.setGoogleId(googleId);
        userRepository.save(user);
        return user;
    }

    private String encryptGoogleId(String googleId) {
        return passwordEncoder.encode(googleId);
    }

    private AuthResponse handleOAuth2FaceBookLogin(String email, String facebookId) {
        User user = findOrFaceBookCreateUser(email, facebookId);
        Session newSession = sessionService.createSession(user);
        Set<Role> roles = user.getRoles();
        String newAccessToken = jwtTokenProvider.generateToken(email, roles);
        String newRefreshToken = jwtTokenProvider.generateRefreshToken(email, roles);
        AuthResponse authResponse = new AuthResponse();
        authResponse.setAccessToken(newAccessToken);
        authResponse.setRefreshToken(newRefreshToken);
        authResponse.setSessionResponse(new SessionResponse(newSession.getId(), newSession.getCreatedAt(), newSession.getExpiresAt()));
        authResponse.setGoogleId(facebookId);
        return authResponse;
    }

    private User findOrFaceBookCreateUser(String email, String facebookId) {
        Optional<User> existingUser = userRepository.findByEmail(email);

        if (existingUser.isPresent()) {
            User user = existingUser.get();
            linkFaceBookOAuthProviderIfNecessary(user, facebookId);
            return user;
        } else {
            return createFaceBookUserWithOAuth(email, facebookId);
        }
    }

    private void linkFaceBookOAuthProviderIfNecessary(User user, String facebookId) {
        if (user.getFacebookId() == null && facebookId != null) {
            user.setGoogleId(facebookId);
            userRepository.save(user);
        } else {
            throw new AppException(ErrorCode.LINKED_ACCOUNT_MISMATCH);
        }
    }

    private User createFaceBookUserWithOAuth(String email, String facebookId) {
        User user = new User();
        user.setEmail(email);
        if (facebookId != null) user.setGoogleId(facebookId);
        userRepository.save(user);
        return user;
    }

    @Transactional
    public AuthResponse linkGoogleAccount(String code) {
        String accessToken = exchangeCodeForAccessToken(code);
        OAuth2User googleUser = fetchUserInfoFromGoogle(accessToken);
        String googleEmail = googleUser.getAttribute("email");
        String googleId = googleUser.getAttribute("sub");
        User existingUser = userRepository.findByEmail(googleEmail)
                .orElseThrow(() -> new AppException(ErrorCode.EMAIL_NOT_EXIST));
        existingUser.setGoogleEmail(googleEmail);
        existingUser.setGoogleId(encryptGoogleId(googleId));
        User updatedUser = userRepository.save(existingUser);
        return userMapper.toAuthResponse(updatedUser);
    }

    private String exchangeCodeForAccessToken(String code) {
        String tokenEndpoint = tokenUri;
        Map<String, String> params = new HashMap<>();
        params.put("client_id", clientId);
        params.put("client_secret", clientSecret);
        params.put("redirect_uri", redirectUri);
        params.put("grant_type", "authorization_code");
        params.put("code", code);
        try {
            var response = restTemplate.postForEntity(tokenEndpoint, params, Map.class);
            var responseBody = response.getBody();
            if (responseBody == null || responseBody.get("access_token") == null) {
                throw new AppException(ErrorCode.INVALID_OAUTH_CODE);
            }
            return (String) responseBody.get("access_token");
        } catch (HttpClientErrorException e) {
            System.out.println("Error response: " + e.getResponseBodyAsString());
            throw new AppException(ErrorCode.INVALID_OAUTH_CODE);
        }
    }

    private OAuth2User fetchUserInfoFromGoogle(String accessToken) {
        String userInfoEndpointUri = userInfoUri;
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        HttpEntity<?> entity = new HttpEntity<>(headers);
        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                userInfoEndpointUri, HttpMethod.GET, entity, new ParameterizedTypeReference<>() {});

        Map<String, Object> userAttributes = response.getBody();
        return new DefaultOAuth2User(
//                Collections.singleton(new SimpleGrantedAuthority("ROLE_USER")), nếu lỗi thì đổi lại là dòng này
                Collections.emptySet(),
                Objects.requireNonNull(userAttributes),
                "sub");
    }

//    private User getCurrentAuthenticatedUser() {
//        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
//        System.out.println("Authentication: " + authentication);
//        if (authentication != null && authentication.isAuthenticated()) {
//            String email = ((UserDetails) authentication.getPrincipal()).getUsername();
//            return userRepository.findByEmail(email)
//                    .orElseThrow(() -> new AppException(ErrorCode.EMAIL_NOT_EXIST));
//        }
//        throw new AppException(ErrorCode.USER_NOT_AUTHENTICATED);
//    }

//    public AuthResponse linkFacebookAccount(String code) {
//        OAuth2User facebookUser = getFacebookUser(code);
//        String googleEmail = facebookUser.getAttribute("email");
//        String facebookId = facebookUser.getAttribute("id");
//        User currentUser = getCurrentAuthenticatedUser();
//        currentUser.setGoogle_email(googleEmail);
//        currentUser.setFacebookId(facebookId);
//        User updatedUser = userRepository.save(currentUser);
//        return userMapper.toAuthResponse(updatedUser);
//    }
}
