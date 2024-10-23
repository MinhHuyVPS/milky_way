package milky_way.controller;

import jakarta.servlet.http.HttpServletRequest;
import milky_way.dto.request.UserRequest;
import milky_way.dto.request.UserRequestLogin;
import milky_way.dto.response.AuthResponse;
import milky_way.dto.response.UserResponse;
import milky_way.exception.AppException;
import milky_way.exception.ErrorCode;
import milky_way.service.AuthService;
import milky_way.service.TokenBlacklistService;
import milky_way.utils.JwtTokenProvider;
import milky_way.utils.Notification;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("api/v1/auth")
public class AuthController {
    @Autowired
    private AuthService authService;

    @Autowired
    private TokenBlacklistService tokenBlacklistService;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody UserRequest userRequest) {
        try {
            UserResponse userResponse = authService.registerUser(userRequest);
            return ResponseEntity.ok(userResponse);
        } catch (AppException e) {
            return ResponseEntity.status(e.getErrorCode().getHttpStatus())
                    .body(e.getErrorCode().getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(ErrorCode.UNKNOWN_ERROR.getHttpStatus())
                    .body(ErrorCode.UNKNOWN_ERROR.getMessage());
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody UserRequestLogin userRequest) {
        try {
            Optional<AuthResponse> authResponse = authService.loginUser(userRequest);
            if (authResponse.isPresent()) {
                return ResponseEntity.ok(authResponse.get());
            } else {
                return ResponseEntity.status(ErrorCode.AUTHENTICATION_FAILED.getHttpStatus())
                        .body(ErrorCode.AUTHENTICATION_FAILED.getMessage());
            }
        } catch (AppException e) {
            return ResponseEntity.status(e.getErrorCode().getHttpStatus())
                    .body(e.getErrorCode().getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(ErrorCode.UNKNOWN_ERROR.getHttpStatus())
                    .body(ErrorCode.UNKNOWN_ERROR.getMessage());
        }
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken(@RequestBody Map<String, String> request) {
        try {
            String refreshToken = request.get("refreshToken");
            if (refreshToken == null || refreshToken.isEmpty()) {
                throw new AppException(ErrorCode.INVALID_REFRESH_TOKEN);
            }

            AuthResponse authResponse = authService.refreshToken(refreshToken);
            return ResponseEntity.ok(authResponse);
        } catch (AppException e) {
            return ResponseEntity.status(e.getErrorCode().getHttpStatus())
                    .body(e.getErrorCode().getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(ErrorCode.UNKNOWN_ERROR.getHttpStatus())
                    .body(ErrorCode.UNKNOWN_ERROR.getMessage());
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletRequest request) {
        String token = tokenBlacklistService.getJwtFromRequest(request);
        LocalDateTime expiryDate = jwtTokenProvider.getClaimsFromJWT(token).getExpiration().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();

        tokenBlacklistService.blacklistToken(token, expiryDate);
        return ResponseEntity.ok().body(Notification.LOGOUT);
    }
}
