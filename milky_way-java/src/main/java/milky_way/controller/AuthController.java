package milky_way.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import milky_way.dto.request.auth.ChangePasswordRequest;
import milky_way.dto.request.auth.OtpRequest;
import milky_way.dto.request.auth.PasswordResetRequest;
import milky_way.dto.request.auth.ResetPasswordRequest;
import milky_way.dto.request.permission.PermissionRequest;
import milky_way.dto.request.role.RoleRequest;
import milky_way.dto.request.user.UserRequest;
import milky_way.dto.request.user.UserRequestLogin;
import milky_way.dto.response.auth.*;
import milky_way.dto.response.role.RoleResponse;
import milky_way.dto.response.user.UserProfileResponse;
import milky_way.dto.response.user.UserResponse;
import milky_way.exception.AppException;
import milky_way.exception.ErrorCode;
import milky_way.service.*;
import milky_way.utils.CustomUserDetails;
import milky_way.utils.JwtTokenProvider;
import milky_way.utils.Notification;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PostAuthorize;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Value;
import java.net.URI;
import java.util.Optional;
import java.util.Set;

@RestController
@CrossOrigin(origins = "http://localhost:3000")
@RequestMapping("api/v1/auth")
@Validated
public class AuthController {

    @Autowired
    private AuthService authService;

    @Autowired
    private TokenBlacklistService tokenBlacklistService;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private UserService userService;

    @Autowired
    private RoleService roleService;

    @Autowired
    private PermissionService permissionService;

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
    public ResponseEntity<?> login(@RequestBody UserRequestLogin userRequest, HttpServletResponse response) {
        try {
            Optional<LoginResponse> authResponse = authService.loginUser(userRequest, response);
            if (authResponse.isPresent()) {
                return ResponseEntity.ok(authResponse.get());
            } else {
                return ResponseEntity.status(ErrorCode.AUTHENTICATION_FAILED.getHttpStatus())
                        .body(ErrorCode.AUTHENTICATION_FAILED.getMessage());
            }
        } catch (AppException e) {
            return ResponseEntity.status(e.getErrorCode().getHttpStatus())
                    .body(e.getErrorCode().getMessage());
        }
    }

    @PostMapping("/refresh")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> refreshToken(HttpServletRequest request) {
        try {
            RefreshResponse authResponse = authService.refreshToken(request);
            return ResponseEntity.ok(authResponse);
        } catch (AppException e) {
            return ResponseEntity.status(e.getErrorCode().getHttpStatus())
                    .body(e.getErrorCode().getMessage());
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<String> logout(HttpServletRequest request, HttpServletResponse response) {
        try {
            authService.logout(request, response);
            return ResponseEntity.ok(Notification.LOGOUT.getValue());
        }catch (AppException e){
            return ResponseEntity.status(e.getErrorCode().getHttpStatus())
                    .body(e.getErrorCode().getMessage());
        }
    }

    @GetMapping("/profile")
    @PreAuthorize("isAuthenticated()")
//    @PostAuthorize("returnObject.body.email == authentication.name")
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
        }
    }

    @GetMapping("/oauth2/login/authorize/google")
    public ResponseEntity<Void> redirectLoginToGoogle() {
        try {
            if (clientId == null || googleRedirectBaseUrl == null || authorRedirectUri == null) {
                throw new AppException(ErrorCode.OAUTH2_CONFIGURATION);
            }
            String redirectUri = authorRedirectUri
                    + "?client_id=" + clientId
                    + "&redirect_uri=" + googleRedirectBaseUrl
                    + "&response_type=code"
                    + "&scope=email%20profile";
            HttpHeaders headers = new HttpHeaders();
            headers.setLocation(URI.create(redirectUri));
            return new ResponseEntity<>(headers, HttpStatus.SEE_OTHER);
        } catch (AppException e) {
            throw new AppException(ErrorCode.OAUTH2_REDIRECT_FAILED);
        }
    }

    @GetMapping("/oauth2/google")
    public ResponseEntity<?> oauth2LoginGoogle(@RequestParam String code, HttpServletResponse response) {
        LoginGoogleResponse loginGoogleResponse = authService.loginWithGoogle(code, response);
        return ResponseEntity.ok().body(loginGoogleResponse);
    }

    @GetMapping("/oauth2/login/authorize/facebook")
    public ResponseEntity<Void> oauth2LoginFacebook() {
        try {
            if (clientFacebookId == null || facebookRedirectBaseUrl == null || authorFacebookRedirectUri == null) {
                throw new AppException(ErrorCode.OAUTH2_CONFIGURATION);
            }
        String redirectUri = authorFacebookRedirectUri
                + "?client_id=" + clientFacebookId
                + "&redirect_uri=" + facebookRedirectBaseUrl
                + "&response_type=code"
                + "&scope=email,public_profile";
        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(URI.create(redirectUri));
        return new ResponseEntity<>(headers, HttpStatus.SEE_OTHER);
        } catch (AppException e) {
            throw new AppException(ErrorCode.OAUTH2_REDIRECT_FAILED);
        }
    }

    @GetMapping("/oauth2/facebook")
    public ResponseEntity<?> oauth2LoginFacebook(@RequestParam String code, HttpServletResponse response) {
        LoginFacebookResponse loginWithFacebook = authService.loginWithFacebook(code, response);
        return ResponseEntity.ok().body(loginWithFacebook);
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
        LinkGoogleResponse linkGoogleAccount = authService.linkGoogleAccount(code);
        return ResponseEntity.ok(linkGoogleAccount);
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
    public ResponseEntity<String> changePassword(@RequestBody ChangePasswordRequest changePasswordRequest, HttpServletRequest request,  HttpServletResponse response) {
        authService.changePassword(changePasswordRequest.getOldPassword(), changePasswordRequest.getNewPassword(), request, response);
        return ResponseEntity.ok(Notification.CHANGED_PASSWORD_SUCCESSFUL.getValue());
    }

    @PostMapping("/add-roles")
    @PreAuthorize("hasRole('ADMIN') and hasAuthority('ADMIN_WRITE')")
    public ResponseEntity<?> addRoles(@RequestBody RoleRequest roleRequest){
        UserResponse response = roleService.addRolesToUser(roleRequest);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/update-roles")
    @PreAuthorize("hasRole('ADMIN') and hasAnyAuthority('ADMIN_WRITE', 'ADMIN_DELETE')")
    public ResponseEntity<?> updateRoles(@RequestBody RoleRequest roleRequest){
        UserResponse response = roleService.updateRolesForUser(roleRequest);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/add-permissions")
    @PreAuthorize("hasRole('ADMIN') and hasAuthority('ADMIN_WRITE')")
    public ResponseEntity<?> addPermissions(@RequestBody PermissionRequest permissionRequest){
        RoleResponse response = permissionService.addPermissionsToRole(permissionRequest);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/add-permissions")
    @PreAuthorize("hasRole('ADMIN') and hasAnyAuthority('ADMIN_WRITE', 'ADMIN_DELETE')")
    public ResponseEntity<?> updatePermissions(@RequestBody PermissionRequest permissionRequest){
        RoleResponse response = permissionService.updatePermissionsForRole(permissionRequest);
        return ResponseEntity.ok(response);
    }
}


