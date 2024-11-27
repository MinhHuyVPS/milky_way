package milky_way.controller;

import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import milky_way.dto.request.auth.ChangePasswordRequest;
import milky_way.dto.request.auth.OtpRequest;
import milky_way.dto.request.auth.PasswordResetRequest;
import milky_way.dto.request.auth.ResetPasswordRequest;
import milky_way.dto.request.user.UserRequest;
import milky_way.dto.request.user.UserRequestLogin;
import milky_way.dto.response.auth.AuthLoginResponse;
import milky_way.dto.response.auth.AuthResponse;
import milky_way.dto.response.user.UserProfileResponse;
import milky_way.dto.response.user.UserResponse;
import milky_way.exception.AppException;
import milky_way.exception.ErrorCode;
import milky_way.service.AuthService;
import milky_way.service.TokenBlacklistService;
import milky_way.utils.CustomUserDetails;
import milky_way.utils.JwtTokenProvider;
import milky_way.utils.Notification;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PostAuthorize;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Value;
import java.net.URI;
import java.util.Locale;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
@RestController
@CrossOrigin(origins = "http://localhost:3000")
@RequestMapping("api/v1/auth")
@Validated
public class AuthController {
    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    @Autowired
    private AuthService authService;

    @Autowired
    private TokenBlacklistService tokenBlacklistService;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Value("${spring.security.oauth2.client.registration.google.client-id}")
    private String clientId;

    @Value("${spring.security.oauth2.client.registration.facebook.client-id}")
    private String clientFacebookId;

    @Value("${spring.security.oauth2.client.registration.google.redirect.uri}")
    private String redirectUris;

    @Value("${spring.security.oauth2.client.provider.google.authorization-uri}")
    private String authorRedirectUri;

    @Value("${spring.security.oauth2.client.provider.facebook.authorization-uri}")
    private String authorFacebookRedirectUri;

    @Value("${google.oauth2.redirect.base-url}")
    private String googleRedirectBaseUrl;

    @Value("${oauth2.redirect.facebook.base-url}")
    private String facebookRedirectBaseUrl;

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody UserRequest userRequest) {
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
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> refreshToken(@RequestHeader("Authorization") String authorizationHeader) {
        try {
            if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
                throw new AppException(ErrorCode.INVALID_REFRESH_TOKEN);
            }
            String refreshToken = authorizationHeader.substring(7);
            AuthLoginResponse authResponse = authService.refreshToken(refreshToken);
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
    public ResponseEntity<String> logout(HttpServletRequest request) {
        String authorizationHeader = request.getHeader("Authorization");
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            return ResponseEntity.badRequest().body("Invalid token format");
        }
        String token = authorizationHeader.substring(7);
        authService.logout(request);
        return ResponseEntity.ok(Notification.LOGOUT.getValue());
    }

    @GetMapping("/profile")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getProfile(HttpServletRequest request) {
        try {
            UserProfileResponse profile = authService.getProfile(request);

            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (profile.getEmail().equals(authentication.getName())) {
                return ResponseEntity.ok(profile);
            } else {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(ErrorCode.ACCESS_DENIED.getMessage());
            }
        } catch (AppException e) {
            return ResponseEntity.status(e.getErrorCode().getHttpStatus())
                    .body(e.getErrorCode().getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(ErrorCode.UNKNOWN_ERROR.getHttpStatus())
                    .body(ErrorCode.UNKNOWN_ERROR.getMessage());
        }
    }

    @GetMapping("/oauth2/login/authorize/google")
    public ResponseEntity<Void> redirectLoginToGoogle() {
        String redirectUri = authorRedirectUri
                + "?client_id=" + clientId
                + "&redirect_uri=" + googleRedirectBaseUrl
                + "&response_type=code"
                + "&scope=email%20profile";
        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(URI.create(redirectUri));
        return new ResponseEntity<>(headers, HttpStatus.SEE_OTHER);
    }

    @GetMapping("/oauth2/google")
    public ResponseEntity<?> oauth2LoginGoogle(@RequestParam String code) {
        AuthResponse authResponse = authService.loginWithGoogle(code);
        return ResponseEntity.ok().body(authResponse);
    }

    @GetMapping("/oauth2/login/authorize/facebook")
    public ResponseEntity<Void> oauth2LoginFacebook() {
        String redirectUri = authorFacebookRedirectUri
                + "?client_id=" + clientFacebookId
                + "&redirect_uri=" + facebookRedirectBaseUrl
                + "&response_type=code"
                + "&scope=email,public_profile";
        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(URI.create(redirectUri));
        return new ResponseEntity<>(headers, HttpStatus.SEE_OTHER);
    }

    @GetMapping("/oauth2/facebook")
    public ResponseEntity<?> oauth2LoginFacebook(@RequestParam String code) {
        AuthResponse authResponse = authService.loginWithFacebook(code);
        return ResponseEntity.ok().body(authResponse);
    }

    @GetMapping("/oauth2/authorize/google")
    public ResponseEntity<Void> redirectToGoogle() {
        String redirectUri = authorRedirectUri
                + "?client_id=" + clientId
                + "&redirect_uri=" + redirectUris
                + "&response_type=code"
                + "&scope=email%20profile";
        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(URI.create(redirectUri));
        return new ResponseEntity<>(headers, HttpStatus.SEE_OTHER);
    }

    @GetMapping("/login-success")
    @ResponseBody
    public String loginSuccess(@AuthenticationPrincipal OidcUser principal) {
        String userName = principal != null ? principal.getFullName() : "User";
        return "Login successful! Welcome, " + userName;
    }

    @GetMapping("/login-failure")
    @ResponseBody
    public String loginFailure() {
        return "Login failed. Please try again.";
    }

    @GetMapping("/oauth2/callback")
    public ResponseEntity<?> handleGoogleCallback(@RequestParam String code) {
        AuthResponse authResponse = authService.linkGoogleAccount(code);
        return ResponseEntity.ok(authResponse);
    }

    @PostMapping("/send-reset-password-email")
    public ResponseEntity<String> sendResetPasswordEmail(@RequestBody PasswordResetRequest request) {
        authService.sendResetPasswordEmail(request.getEmail());
        return ResponseEntity.ok(Notification.RESET_PASSWORD.getValue());
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<String> verifyOtp(@RequestBody OtpRequest request) {
        String otp = authService.verifyOtp(request.getOtp());
        return ResponseEntity.ok(otp);
    }

    @PostMapping("/reset-password")
    public ResponseEntity<String> resetPassword(@RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request.getOtp(), request.getNewPassword());
        return ResponseEntity.ok(Notification.RESET_PASSWORD_SUCCESSFUL.getValue());
    }

    @PostMapping("/change-password")
    public ResponseEntity<String> changePassword(@AuthenticationPrincipal CustomUserDetails userDetails, @RequestBody ChangePasswordRequest request) {
        String email = userDetails.getUsername();
        authService.changePassword(email, request.getOldPassword(), request.getNewPassword());
        return ResponseEntity.ok("Password changed successfully.");
    }
}
