package milky_way.service;

import jakarta.servlet.http.HttpServletRequest;
import milky_way.dto.request.UserRequest;
import milky_way.dto.request.UserRequestLogin;
import milky_way.dto.response.AuthResponse;
import milky_way.dto.response.SessionResponse;
import milky_way.dto.response.UserProfileResponse;
import milky_way.dto.response.UserResponse;
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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;

@Service
public class AuthService {

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

    public UserResponse registerUser(UserRequest userRequest) {
        if (userRepository.existsByEmail(userRequest.getEmail())) {
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
        String refreshToken = jwtTokenProvider.generateRefreshToken(user.get().getEmail());
        Session session = sessionService.createSession(user.get());
        AuthResponse authResponse = new AuthResponse();
        authResponse.setAccessToken(accessToken);
        authResponse.setRefreshToken(refreshToken);
        authResponse.setSessionResponse(new SessionResponse(session.getId(), session.getCreatedAt(), session.getExpiresAt()));
        return Optional.of(authResponse);
    }

    public AuthResponse refreshToken(String refreshToken) {
        if (!jwtTokenProvider.validateToken(refreshToken) || jwtTokenProvider.isTokenExpired(refreshToken)) {
            throw new AppException(ErrorCode.INVALID_REFRESH_TOKEN);
        }
        String email = jwtTokenProvider.getEmailFromJWT(refreshToken);
        Optional<User> user = userRepository.findByEmail(email);
        if (user.isEmpty()) {
            throw new AppException(ErrorCode.EMAIL_NOT_EXIST);
        }
        if (!sessionService.isSessionValid(email)) {
            Session newSession = sessionService.createSession(user.get());
            Set<Role> roles = user.get().getRoles();
            String newAccessToken = jwtTokenProvider.generateToken(email, roles);
            AuthResponse authResponse = new AuthResponse();
            authResponse.setAccessToken(newAccessToken);
            authResponse.setRefreshToken(refreshToken);
            authResponse.setSessionResponse(new SessionResponse(newSession.getId(), newSession.getCreatedAt(), newSession.getExpiresAt()));
            return authResponse;
        }
        Set<Role> roles = user.get().getRoles();
        String newAccessToken = jwtTokenProvider.generateToken(email, roles);
        AuthResponse authResponse = new AuthResponse();
        authResponse.setAccessToken(newAccessToken);
        authResponse.setRefreshToken(refreshToken);
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
}
