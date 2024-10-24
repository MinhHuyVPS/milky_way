package milky_way.controller;

import jakarta.servlet.http.HttpServletRequest;
import milky_way.dto.request.UserRequest;
import milky_way.dto.request.UserRequestLogin;
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
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PostAuthorize;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

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

    @GetMapping("/google-login")
    public ResponseEntity<AuthResponse> googleLogin(@RequestParam String code) {
        AuthResponse authResponse = authService.handleGoogleLogin(code);
        return ResponseEntity.ok(authResponse);
    }

    @GetMapping("/facebook-login")
    public ResponseEntity<AuthResponse> facebookLogin(@RequestParam String code) {
        AuthResponse authResponse = authService.handleFacebookLogin(code);
        return ResponseEntity.ok(authResponse);
    }

    @PostMapping("/link-google")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<String> linkGoogleAccount(@RequestParam String code) {
        authService.linkGoogleAccount(code);
        return ResponseEntity.ok("Google account linked successfully.");
    }

    @PostMapping("/link-google")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<String> linkFacebookAccount(@RequestParam String code) {
        authService.linkFacebookAccount(code);
        return ResponseEntity.ok("Facebook account linked successfully.");
    }

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
