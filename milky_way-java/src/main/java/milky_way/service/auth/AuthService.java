package milky_way.service.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.transaction.Transactional;
import milky_way.dto.request.user.UserRequest;
import milky_way.dto.request.user.UserRequestLogin;
import milky_way.dto.response.auth.*;
import milky_way.dto.response.permission.PermissionResponse;
import milky_way.dto.response.session.SessionResponse;
import milky_way.dto.response.user.UserProfileResponse;
import milky_way.dto.response.user.UserResponse;
import milky_way.entity.Permission;
import milky_way.entity.Role;
import milky_way.entity.Session;
import milky_way.entity.User;
import milky_way.exception.AppException;
import milky_way.exception.ErrorCode;
import milky_way.mapper.UserMapper;
import milky_way.repository.PermissionRepository;
import milky_way.repository.RoleRepository;
import milky_way.repository.UserRepository;
import milky_way.service.config.RedisService;
import milky_way.service.permission.PermissionService;
import milky_way.service.role.RoleService;
import milky_way.service.session.SessionService;
import milky_way.service.token.TokenBlacklistService;
import milky_way.service.token.TokenService;
import milky_way.utils.config.JwtTokenProvider;
import milky_way.utils.config.OtpStorage;
import milky_way.utils.cookie.JwtAuthenticationUtils;
import milky_way.utils.enums.Notification;
import milky_way.utils.enums.Permissions;
import milky_way.utils.enums.UserRole;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.beans.factory.annotation.Value;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AuthService {
    @Value("${spring.security.oauth2.client.registration.google.client-id}")
    private String clientId;

    @Value("${spring.security.oauth2.client.registration.facebook.client-id}")
    private String clientFacebookId;

    @Value("${spring.security.oauth2.client.registration.google.redirect.uri}")
    private String redirectUri;

    @Value("${spring.security.oauth2.client.registration.google.client-secret}")
    private String clientSecret;

    @Value("${spring.security.oauth2.client.registration.facebook.client-secret}")
    private String clientFacebookSecret;

    @Value("${spring.security.oauth2.client.provider.google.token-uri}")
    private String tokenUri;

    @Value("${spring.security.oauth2.client.provider.facebook.token-uri}")
    private String facebookTokenUri;

    @Value("${spring.security.oauth2.client.provider.google.user-info-uri}")
    private String userInfoUri;

    @Value("${spring.security.oauth2.client.provider.facebook.user-info-uri}")
    private String facebookUserInfoUri;

    @Value("${google.oauth2.redirect.base-url}")
    private String googleRedirectBaseUrl;

    @Value("${oauth2.redirect.facebook.base-url}")
    private String facebookRedirectBaseUrl;

    @Value("${jwt.signerKey}")
    private String SECRET_KEY;

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

    @Autowired
    private JavaMailSender mailSender;

    @Autowired
    private RedisService redisTokenService;

    @Autowired
    private TokenService tokenService;

    @Autowired
    private RoleService roleService;

    @Autowired
    private JwtAuthenticationUtils jwtAuthenticationUtils;

    @Autowired
    private PermissionRepository permissionRepository;

    @Autowired
    private PermissionService permissionService;

    @Transactional
    public UserResponse registerUser(UserRequest userRequest) {
        if (userRepository.existsByEmail(userRequest.getEmail())) {
            throw new AppException(ErrorCode.EMAIL_EXIST);
        }
        if(userRepository.existsByGoogleEmail(userRequest.getEmail())){
            throw new AppException(ErrorCode.EMAIL_EXIST);
        }
        if(userRepository.existsByFacebookEmail(userRequest.getEmail())){
            throw new AppException(ErrorCode.EMAIL_EXIST);
        }
        User user = userMapper.toUser(userRequest);
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        Role userRole = roleRepository.findByName(UserRole.USER.getValue())
                .orElseGet(() -> {
                    Role newRole = Role.builder()
                            .name(UserRole.USER.getValue())
                            .description(Notification.REGISTER.getValue())
                            .build();
                    roleRepository.save(newRole);
                    Set<String> permissionNames = new HashSet<>(Arrays.asList(
                            Permissions.USER_READ.getValue(),
                            Permissions.USER_WRITE.getValue(),
                            Permissions.USER_DELETE.getValue()
                    ));
                    Set<Permission> userPermissions = new HashSet<>(permissionRepository.findByNameIn(permissionNames));
                    newRole.setPermissions(userPermissions);
                    return roleRepository.save(newRole);
                });
        user.setRoles(new HashSet<>(Collections.singletonList(userRole)));
        User saveUser = userRepository.save(user);
        return userMapper.toUserResponse(saveUser);
    }

    @Transactional
    public Optional<LoginResponse> loginUser(UserRequestLogin userRequest, HttpServletResponse response) {
        Optional<User> user = Optional.ofNullable(userRepository.findByEmail(userRequest.getEmail())
                .orElseThrow(() -> new AppException(ErrorCode.EMAIL_NOT_EXIST)));
        if (!passwordEncoder.matches(userRequest.getPassword(), user.get().getPassword())) {
            throw new AppException(ErrorCode.PASSWORD_INCORRECT);
        }
        if(user.get().getDeleteDate() != null){
            throw new AppException(ErrorCode.USER_NOT_FOUND);
        }
        Set<Role> roles = user.get().getRoles();
        Set<String> roleIds = roles.stream()
                .map(Role::getId)
                .collect(Collectors.toSet());
        Set<PermissionResponse> permissions = permissionService.findPermissionsByRolesIn(roleIds);
        String accessToken = jwtTokenProvider.generateToken(user.get().getId() ,user.get().getEmail(), roles, permissions);
        String refreshToken = jwtTokenProvider.generateRefreshToken(user.get().getId() ,user.get().getEmail(), roles, permissions);
        redisTokenService.saveAccessToken(user.get().getEmail(), accessToken);
        redisTokenService.saveRefreshToken(user.get().getEmail(), refreshToken);
        tokenService.deleteRefreshTokens(user.get());
        tokenService.saveRefreshToken(user.get(), refreshToken);
        jwtAuthenticationUtils.addRefreshTokenCookie(refreshToken, response);
        sessionService.invalidateAllSessions(user.get().getEmail());
        Session session = sessionService.createSession(user.get());
        LoginResponse loginResponse = new LoginResponse();
        loginResponse.setAccessToken(accessToken);
        loginResponse.setSessionResponse(new SessionResponse(session.getId(), session.getCreatedDate(), session.getExpiresDate()));
        return Optional.of(loginResponse);
    }

    @Transactional
    public RefreshResponse refreshToken(HttpServletRequest request, HttpServletResponse response) {
        String refreshToken = jwtAuthenticationUtils.extractRefreshTokenFromCookie(request);
        if (refreshToken == null || !jwtTokenProvider.validateToken(refreshToken) || !jwtTokenProvider.isRefreshToken(refreshToken)) {
            throw new AppException(ErrorCode.INVALID_REFRESH_TOKEN);
        }
        String email = jwtTokenProvider.getEmailFromJWT(refreshToken);
        String id = jwtTokenProvider.getIdFromJWT(refreshToken);
        Optional<User> user = userRepository.findByEmailAndId(email, id);
        if (user.isEmpty()) {
            throw new AppException(ErrorCode.EMAIL_NOT_EXIST);
        }
        if(user.get().getDeleteDate() != null){
            throw new AppException(ErrorCode.USER_NOT_FOUND);
        }
        Set<Role> roles = user.get().getRoles();
        Set<String> roleIds = roles.stream()
                .map(Role::getId)
                .collect(Collectors.toSet());
        Set<PermissionResponse> permissions = permissionService.findPermissionsByRolesIn(roleIds);
        tokenBlacklistService.blacklistToken(refreshToken, jwtTokenProvider.getExpiryDateFromToken(refreshToken));
        String newAccessToken = jwtTokenProvider.generateToken(id ,email, roles, permissions);
        String newRefreshToken = jwtTokenProvider.generateRefreshToken(id ,email, roles, permissions);
        RefreshResponse refreshResponse = new RefreshResponse();
        refreshResponse.setAccessToken(newAccessToken);
        Optional<Session> currentSession = sessionService.getSession(email);
        if (currentSession.isPresent()) {
            Session session = currentSession.get();
            refreshResponse.setSessionResponse(new SessionResponse(session.getId(), session.getCreatedDate(), session.getExpiresDate()));
        } else {
            Session newSession = sessionService.createSession(user.get());
            refreshResponse.setSessionResponse(new SessionResponse(newSession.getId(), newSession.getCreatedDate(), newSession.getExpiresDate()));
        }
        if (refreshResponse.getSessionResponse() == null) {
            throw new AppException(ErrorCode.INVALID_RESPONSE_CONTENT);
        }
        jwtAuthenticationUtils.addRefreshTokenCookie(newRefreshToken, response);
        return refreshResponse;
    }

    @Transactional
    public void logout(HttpServletRequest request, HttpServletResponse response) {
        String accessToken = extractAndValidateAccessToken(request);
        String email = jwtTokenProvider.getEmailFromJWT(accessToken);
        invalidateAccessTokenAndSession(accessToken, email);

        String refreshToken = jwtAuthenticationUtils.extractRefreshTokenFromCookie(request);
        processRefreshToken(refreshToken, response);
    }

    @Transactional
    public UserProfileResponse getProfile(HttpServletRequest request) {
        String token = tokenBlacklistService.getJwtFromRequest(request);
        if (token == null || !jwtTokenProvider.validateToken(token) || !jwtTokenProvider.isAccessToken(token)) {
            throw new AppException(ErrorCode.INVALID_TOKEN);
        }
        String email = jwtTokenProvider.getEmailFromJWT(token);
        String id = jwtTokenProvider.getIdFromJWT(token);
        if (!sessionService.isSessionValid(email)) {
            throw new AppException(ErrorCode.SESSION_INVALID);
        }
        User user = userRepository.findByEmailAndId(email, id)
                .orElseThrow(() -> new AppException(ErrorCode.EMAIL_NOT_EXIST));
        if(user.getDeleteDate() != null){
            throw new AppException(ErrorCode.USER_NOT_FOUND);
        }
        String userEmail = getUserEmail(user, email);
        if (!userEmail.equals(email)) {
            throw new AppException(ErrorCode.UNAUTHORIZED_ACCESS);
        }
        return new UserProfileResponse(user.getUsername(), userEmail, user.getFirstname(), user.getLastname(), user.getPhone(), user.getAddress(), user.getBirthday());
    }

    @Transactional
    public LoginGoogleResponse loginWithGoogle(String code, HttpServletResponse response) {
        String accessToken = exchangeCodeForAccessTokenLogin(code);
        OAuth2User googleUser = fetchUserInfoFromGoogle(accessToken);
        String googleEmail = googleUser.getAttribute("email");
        String googleId = googleUser.getAttribute("sub");
        return handleOAuth2GoogleLogin(googleEmail, googleId, response);
    }

    private String exchangeCodeForAccessTokenLogin(String code) {
        String tokenEndpoint = tokenUri;
        Map<String, String> params = new HashMap<>();
        params.put("client_id", clientId);
        params.put("client_secret", clientSecret);
        params.put("redirect_uri", googleRedirectBaseUrl);
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
            throw new AppException(ErrorCode.INVALID_OAUTH_CODE);
        }
        catch (Exception e) {
            throw new AppException(ErrorCode.UNKNOWN_ERROR);
        }
    }

    private LoginGoogleResponse handleOAuth2GoogleLogin(String googleEmail, String googleId, HttpServletResponse response) {
        User user = findOrGoogleCreateUser(googleEmail, googleId);
        sessionService.invalidateAllSessions(user.getEmail());
        Session newSession = sessionService.createSession(user);
        Set<Role> roles = user.getRoles();
        Set<String> roleIds = roles.stream()
                .map(Role::getId)
                .collect(Collectors.toSet());
        Set<PermissionResponse> permissions = permissionService.findPermissionsByRolesIn(roleIds);
        String newAccessToken = jwtTokenProvider.generateToken(user.getId(), googleEmail, roles, permissions);
        String newRefreshToken = jwtTokenProvider.generateRefreshToken(user.getId(),googleEmail, roles, permissions);
        redisTokenService.saveAccessToken(user.getEmail(), newAccessToken);
        redisTokenService.saveRefreshToken(user.getEmail(), newRefreshToken);
        tokenService.deleteRefreshTokens(user);
        tokenService.saveRefreshToken(user, newRefreshToken);
        jwtAuthenticationUtils.addRefreshTokenCookie(newRefreshToken, response);
        LoginGoogleResponse loginGoogleResponse = new LoginGoogleResponse();
        loginGoogleResponse.setAccessToken(newAccessToken);
        loginGoogleResponse.setSessionResponse(new SessionResponse(newSession.getId(), newSession.getCreatedDate(), newSession.getExpiresDate()));
        loginGoogleResponse.setGoogleId(user.getGoogleId());
        return loginGoogleResponse;
    }

    private User findOrGoogleCreateUser(String googleEmail, String googleId) {
        Optional<User> existingUser = userRepository.findByGoogleEmail(googleEmail);
        if (existingUser.isPresent()) {
            if(existingUser.get().getDeleteDate() != null){
                throw new AppException(ErrorCode.USER_NOT_FOUND);
            }
            User user = existingUser.get();
            linkGoogleOAuthProviderIfNecessary(user, googleId);
            return user;
        } else {
            return createGoogleUserWithOAuth(googleEmail, googleId);
        }
    }

    private void linkGoogleOAuthProviderIfNecessary(User user, String googleId) {
        String encryptedGoogleId = encryptId(googleId);
        if (user.getGoogleId() == null && googleId != null) {
            user.setGoogleId(encryptedGoogleId);
            userRepository.save(user);
        } else if (user.getGoogleId() != null &&  !passwordEncoder.matches(googleId, user.getGoogleId())) {
            throw new AppException(ErrorCode.LINKED_ACCOUNT_MISMATCH);
        }
    }

    private User createGoogleUserWithOAuth(String googleEmail, String googleId) {
        String encryptedGoogleId = encryptId(googleId);
        User user = new User();
        user.setGoogleEmail(googleEmail);
        if (encryptedGoogleId != null) user.setGoogleId(encryptedGoogleId);
        Role userRole = roleRepository.findByName(UserRole.USER.getValue())
                .orElseGet(() -> {
                    Role newRole = Role.builder()
                            .name(UserRole.USER.getValue())
                            .description(Notification.REGISTER.getValue())
                            .build();
                    roleRepository.save(newRole);
                    Set<String> permissionNames = new HashSet<>(Arrays.asList(
                            Permissions.USER_READ.getValue(),
                            Permissions.USER_WRITE.getValue(),
                            Permissions.USER_DELETE.getValue()
                    ));
                    Set<Permission> userPermissions = new HashSet<>(permissionRepository.findByNameIn(permissionNames));
                    newRole.setPermissions(userPermissions);
                    return roleRepository.save(newRole);
                });
        user.setRoles(new HashSet<>(Collections.singletonList(userRole)));
        userRepository.save(user);
        return user;
    }

    private OAuth2User fetchUserInfoFromGoogle(String accessToken) {
        String userInfoEndpointUri = userInfoUri;
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        HttpEntity<?> entity = new HttpEntity<>(headers);
        try {
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    userInfoEndpointUri, HttpMethod.GET, entity, new ParameterizedTypeReference<>() {});

            Map<String, Object> userAttributes = response.getBody();
            if (userAttributes == null) {
                throw new AppException(ErrorCode.INVALID_OAUTH_CODE);
            }
            return new DefaultOAuth2User(
//                Collections.singleton(new SimpleGrantedAuthority("ROLE_USER")), nếu lỗi thì đổi lại là dòng này
                    Collections.emptySet(),
                    Objects.requireNonNull(userAttributes),
                    "sub");
        } catch (HttpClientErrorException e) {
            throw new AppException(ErrorCode.INVALID_ACCESS_TOKEN);
        }
    }

    private String encryptId(String googleId) {
        return passwordEncoder.encode(googleId);
    }

    @Transactional
    public LoginFacebookResponse loginWithFacebook(String code, HttpServletResponse response) {
        String accessToken = exchangeCodeForAccessTokenLoginFacebook(code);
        OAuth2User facebookUser = fetchUserInfoFromFacebook(accessToken);
        String facebookEmail = facebookUser.getAttribute("email");
        String facebookId = facebookUser.getAttribute("id");
        String facebookName = facebookUser.getAttribute("name");
        Map<String, Object> pictureData = (Map<String, Object>) facebookUser.getAttribute("picture");
        String avatar = null;
        if (pictureData != null) {
            Map<String, Object> data = (Map<String, Object>) pictureData.get("data");
            if (data != null) {
                avatar = (String) data.get("url");
            }
        }
        return handleOAuth2FaceBookLogin(facebookEmail, facebookId, facebookName, avatar, response);
    }

    private String exchangeCodeForAccessTokenLoginFacebook(String code) {
        String tokenEndpoint = facebookTokenUri; //https://graph.facebook.com/v21.0/oauth/access_token
        Map<String, String> params = new HashMap<>();
        params.put("client_id", clientFacebookId);
        params.put("client_secret", clientFacebookSecret);
        params.put("redirect_uri", facebookRedirectBaseUrl);
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
            throw new AppException(ErrorCode.INVALID_OAUTH_CODE);
        }catch (Exception e) {
            throw new AppException(ErrorCode.UNKNOWN_ERROR);
        }
    }

    private OAuth2User fetchUserInfoFromFacebook(String accessToken) {
        String userInfoEndpointUri = facebookUserInfoUri; // https://graph.facebook.com/v12.0/me?fields=id,name,email,picture
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        HttpEntity<?> entity = new HttpEntity<>(headers);
        try {
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    userInfoEndpointUri, HttpMethod.GET, entity, new ParameterizedTypeReference<>() {});
            Map<String, Object> userAttributes = response.getBody();

            if (userAttributes == null) {
                throw new RuntimeException("No user attributes found in response");
            }
            Set<GrantedAuthority> authorities = new HashSet<>();
            authorities.add(new SimpleGrantedAuthority(UserRole.USER.getValue()));
            return new DefaultOAuth2User(
                    Collections.emptySet(),
                    userAttributes,
                    "id"
            );
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Error fetching user info from Facebook", e);
        }
    }

    private LoginFacebookResponse handleOAuth2FaceBookLogin(String email, String facebookId, String facebookName, String avatar, HttpServletResponse response) {
        User user = findOrFaceBookCreateUser(email, facebookId, facebookName, avatar);
        sessionService.invalidateAllSessions(user.getEmail());
        Session newSession = sessionService.createSession(user);
        Set<Role> roles = user.getRoles();
        Set<String> roleIds = roles.stream()
                .map(Role::getId)
                .collect(Collectors.toSet());
        Set<PermissionResponse> permissions = permissionService.findPermissionsByRolesIn(roleIds);
        String newAccessToken = jwtTokenProvider.generateToken(user.getId(), email, roles, permissions);
        String newRefreshToken = jwtTokenProvider.generateRefreshToken(user.getId(),email, roles, permissions);
        redisTokenService.saveAccessToken(user.getEmail(), newAccessToken);
        redisTokenService.saveRefreshToken(user.getEmail(), newRefreshToken);
        tokenService.deleteRefreshTokens(user);
        tokenService.saveRefreshToken(user, newRefreshToken);
        jwtAuthenticationUtils.addRefreshTokenCookie(newRefreshToken, response);
        LoginFacebookResponse loginFacebookResponse = new LoginFacebookResponse();
        loginFacebookResponse.setAccessToken(newAccessToken);
        loginFacebookResponse.setSessionResponse(new SessionResponse(newSession.getId(), newSession.getCreatedDate(), newSession.getExpiresDate()));
        loginFacebookResponse.setFacebookId(user.getFacebookId());
        return loginFacebookResponse;
    }

    private User findOrFaceBookCreateUser(String email, String facebookId, String facebookName, String avatar) {
        Optional<User> existingUser = userRepository.findByEmail(email);
        if (existingUser.isPresent()) {
            if(existingUser.get().getDeleteDate() != null){
                throw new AppException(ErrorCode.USER_NOT_FOUND);
            }
            User user = existingUser.get();
            linkFaceBookOAuthProviderIfNecessary(user, facebookId, facebookName, avatar);
            return user;
        } else {
            return createFaceBookUserWithOAuth(email, facebookId, facebookName, avatar);
        }
    }

    private void linkFaceBookOAuthProviderIfNecessary(User user, String facebookId, String facebookName, String avatar) {
        String encryptedFacebookId = encryptId(facebookId);
        if (user.getFacebookId() == null && facebookId != null) {
            user.setFacebookId(encryptedFacebookId);
        }
        if(user.getUsername() == null && facebookName != null){
            user.setUsername(facebookName);
        }
        if(user.getAvatar() == null && avatar != null){
            user.setAvatar(avatar);
        }
        else if (user.getFacebookId() != null &&  !passwordEncoder.matches(facebookId, user.getFacebookId())) {
            throw new AppException(ErrorCode.LINKED_ACCOUNT_MISMATCH);
        }
        userRepository.save(user);
    }

    private User createFaceBookUserWithOAuth(String email, String facebookId, String facebookName, String avatar) {
        String encryptedFacebookId = encryptId(facebookId);
        User user = new User();
        user.setEmail(email);
        user.setFacebookEmail(email);
        user.setUsername(facebookName);
        user.setAvatar(avatar);
        if (facebookId != null) user.setFacebookId(encryptedFacebookId);
        Role userRole = roleRepository.findByName(UserRole.USER.getValue())
                .orElseGet(() -> {
                    Role newRole = Role.builder()
                            .name(UserRole.USER.getValue())
                            .description(Notification.REGISTER.getValue())
                            .build();
                    roleRepository.save(newRole);
                    Set<String> permissionNames = new HashSet<>(Arrays.asList(
                            Permissions.USER_READ.getValue(),
                            Permissions.USER_WRITE.getValue(),
                            Permissions.USER_DELETE.getValue()
                    ));
                    Set<Permission> userPermissions = new HashSet<>(permissionRepository.findByNameIn(permissionNames));
                    newRole.setPermissions(userPermissions);
                    return roleRepository.save(newRole);
                });
        user.setRoles(new HashSet<>(Collections.singletonList(userRole)));
        return userRepository.save(user);
    }

    @Transactional
    public LinkGoogleResponse linkGoogleAccount(String code) {
        String accessToken = exchangeCodeForAccessToken(code);
        OAuth2User googleUser = fetchUserInfoFromGoogle(accessToken);
        String googleEmail = googleUser.getAttribute("email");
        String googleId = googleUser.getAttribute("sub");
        String encryptedGoogleId = encryptId(googleId);
        User existingUser = userRepository.findByEmail(googleEmail)
                .orElseThrow(() -> new AppException(ErrorCode.EMAIL_NOT_EXIST));
        existingUser.setGoogleEmail(googleEmail);
        existingUser.setGoogleId(encryptId(encryptedGoogleId));
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

    private String generateOtpToken() {
        SecureRandom random = new SecureRandom();
        int otp = 100000 + random.nextInt(900000);
        return String.valueOf(otp);
    }

    @Transactional
    public void sendResetPasswordEmail(String email){
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        String opt = generateOtpToken();
        OtpStorage.storeOtp(email, opt);
        SimpleMailMessage mailMessage = new SimpleMailMessage();
        mailMessage.setTo(user.getEmail());
        mailMessage.setSubject(Notification.RESET_PASSWORD.getValue());
        mailMessage.setText(Notification.TEXT_RESET_PASSWORD.getValue() + opt);
        mailSender.send(mailMessage);
    }

    @Transactional
    public String  verifyOtp(String otp) {
        String email = OtpStorage.getEmailByOtp(otp);
        if (email == null) {
            throw new AppException(ErrorCode.INVALID_OTP);
        }
        return otp;
    }

    @Transactional
    public void resetPassword(String otp, String newPassword) {
        String email = OtpStorage.getEmailByOtp(otp);
        if (email == null) {
            throw new AppException(ErrorCode.INVALID_OTP);
        }
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        user.setPassword(passwordEncoder.encode(newPassword));
        OtpStorage.removeOtp(email);
        userRepository.save(user);
    }

    @Transactional
    public void changePassword(String oldPassword, String newPassword, HttpServletRequest request, HttpServletResponse response) {
        String accessToken = extractAndValidateAccessToken(request);
        String email = jwtTokenProvider.getEmailFromJWT(accessToken);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new AppException(ErrorCode.INVALID_OLD_PASSWORD);
        }
        if(user.getDeleteDate() != null){
            throw new AppException(ErrorCode.USER_NOT_FOUND);
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        invalidateAccessTokenAndSession(accessToken, email);

        String refreshToken = jwtAuthenticationUtils.extractRefreshTokenFromCookie(request);
        processRefreshToken(refreshToken, response);
    }

    private String getUserEmail(User user, String requestedEmail) {
        if (user.getEmail() != null && user.getEmail().equals(requestedEmail)) {
            return user.getEmail();
        } else if (user.getGoogleEmail() != null && user.getGoogleEmail().equals(requestedEmail)) {
            return user.getGoogleEmail();
        } else if (user.getFacebookEmail() != null && user.getFacebookEmail().equals(requestedEmail)) {
            return user.getFacebookEmail();
        } else {
            throw new AppException(ErrorCode.UNAUTHORIZED_ACCESS);
        }
    }

    private void validateAccessToken(String accessToken) {
        if (!jwtTokenProvider.validateToken(accessToken) || !jwtTokenProvider.isAccessToken(accessToken)) {
            throw new AppException(ErrorCode.INVALID_TOKEN);
        }
    }

    private String extractAndValidateAccessToken(HttpServletRequest request) {
        String accessToken = tokenService.extractAccessToken(request);
        validateAccessToken(accessToken);
        return accessToken;
    }

    private void processRefreshToken(String refreshToken, HttpServletResponse response) {
        if (refreshToken != null) {
            LocalDateTime refreshTokenExpiryDate = jwtTokenProvider.getExpiryDateFromToken(refreshToken);
            tokenBlacklistService.blacklistToken(refreshToken, refreshTokenExpiryDate);
            String email = jwtTokenProvider.getEmailFromJWT(refreshToken);
            sessionService.invalidateSession(email);
            redisTokenService.invalidateToken(null, refreshToken);
            jwtAuthenticationUtils.deleteRefreshTokenCookie(response);
        } else {
            throw new AppException(ErrorCode.TOKEN_NOT_FOUND);
        }
    }

    private void invalidateAccessTokenAndSession(String accessToken, String email) {
        LocalDateTime accessTokenExpiryDate = jwtTokenProvider.getExpiryDateFromToken(accessToken);
        tokenBlacklistService.blacklistToken(accessToken, accessTokenExpiryDate);
        sessionService.invalidateSession(email);
        redisTokenService.invalidateToken(accessToken, null);
    }


}
