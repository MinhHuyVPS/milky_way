package milky_way.controller;

import jakarta.servlet.http.HttpServletRequest;
import milky_way.dto.request.UserRequest;
import milky_way.dto.request.UserRequestLogin;
import milky_way.dto.response.AuthLoginResponse;
import milky_way.dto.response.AuthResponse;
import milky_way.dto.response.UserProfileResponse;
import milky_way.dto.response.UserResponse;
import milky_way.exception.AppException;
import milky_way.exception.ErrorCode;
import milky_way.service.AuthService;
import milky_way.service.TokenBlacklistService;
import milky_way.utils.JwtTokenProvider;
import milky_way.utils.Notification;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PostAuthorize;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Value;
import java.net.URI;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
@RestController
@CrossOrigin(origins = "http://localhost:3000")
@RequestMapping("api/v1/auth")
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

    @Value("${spring.security.oauth2.client.registration.google.redirect.uri}")
    private String redirectUris;

    @Value("${spring.security.oauth2.client.provider.google.authorization-uri}")
    private String authorRedirectUri;

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
    public ResponseEntity<?> refreshToken(@RequestHeader("Authorization") String authorizationHeader) {
        logger.info("Refreshing token");
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
        authService.logout(request);
        return ResponseEntity.ok(Notification.LOGOUT.getValue());
    }

    @GetMapping("/profile")
    @PreAuthorize("isAuthenticated()")
    @PostAuthorize("returnObject.username == authentication.name")
    public ResponseEntity<?> getProfile(HttpServletRequest request) {
        try {
            UserProfileResponse profile = authService.getProfile(request);
            return ResponseEntity.ok(profile);
        } catch (AppException e) {
            return ResponseEntity.status(e.getErrorCode().getHttpStatus())
                    .body(e.getErrorCode().getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(ErrorCode.UNKNOWN_ERROR.getHttpStatus())
                    .body(ErrorCode.UNKNOWN_ERROR.getMessage());
        }
    }

    @GetMapping("/oauth2/google")
    public ResponseEntity<?> oauth2LoginGoogle(OAuth2AuthenticationToken authentication) {
        OAuth2User oAuth2User = authentication.getPrincipal();
        String email = oAuth2User.getAttribute("email");
        String googleId = oAuth2User.getAttribute("sub");

        AuthResponse authResponse = authService.loginWithGoogle(email, googleId);
        return ResponseEntity.ok().body(authResponse);
    }

    @GetMapping("/oauth2/facebook")
    public ResponseEntity<?> oauth2LoginFacebook(OAuth2AuthenticationToken authentication) {
        OAuth2User oAuth2User = authentication.getPrincipal();
        String email = oAuth2User.getAttribute("email");
        String facebookId = oAuth2User.getAttribute("id");

        AuthResponse authResponse = authService.loginWithFacebook(email, facebookId);
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

    @GetMapping("/oauth2/callback")
    public ResponseEntity<?> handleGoogleCallback(@RequestParam String code) {
        AuthResponse authResponse = authService.linkGoogleAccount(code);
        return ResponseEntity.ok(authResponse);
    }

//    @PostMapping("/link-google")
//    @PreAuthorize("isAuthenticated()")
//    public ResponseEntity<?> linkFacebookAccount(@RequestParam String code) {
//        try{
//            AuthResponse authResponse = authService.linkFacebookAccount(code);
//            return ResponseEntity.ok(authResponse);
//        }catch (AppException e){
//            return ResponseEntity.status(ErrorCode.FACEBOOK_LINK_FOUND.getHttpStatus())
//                    .body(ErrorCode.FACEBOOK_LINK_FOUND.getMessage());
//        }
//    }

//    public function changePassword(UserChangePassword $userChangePassword, $id){
//        return $this->authService->changePassword($id, $userChangePassword->validated());
//    }
//    public function sendResetLinkEmail(UserSendResetLinkEmail $userSendResetLinkEmail){
//        try{
//            return $this->authService->sendResetLinkEmail($userSendResetLinkEmail->validated());
//        }catch(\Throwable $th){
//            return response()->json(['message' => ErrorCode::SEND_EMAIL, 'code' => Response::HTTP_NOT_FOUND]);
//        }
//    }
//    public function resetPassword(UserResetPassword $userResetPassword){
//        try{
//            return $this->authService->resetPassword($userResetPassword->validated());
//        }catch(\Throwable $th){
//            return response()->json(['message' => ErrorCode::RESET_PASSWORD, 'code' => Response::HTTP_NOT_FOUND]);
//        }
//    }
}
