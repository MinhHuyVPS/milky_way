package milky_way.exception;

import lombok.*;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ErrorCode {
    INVALID_OLD_PASSWORD(412, "Invalid old password", HttpStatus.BAD_REQUEST),
    INVALID_OTP(411, "invalid otp",  HttpStatus.NOT_FOUND),
    BAD_REQUEST(400, "bad request", HttpStatus.BAD_REQUEST),
    INVALID_RESPONSE_CONTENT(1000,"loi deo alm", HttpStatus.NO_CONTENT),
    INVALID_OAUTH_CODE(400, "Access token cannot be obtained from authentication code" , HttpStatus.NOT_FOUND),
    INVALID_ACCESS_TOKEN(405, "Failed to fetch user information with provided token",HttpStatus.NOT_FOUND),
    FACEBOOK_LINK_FOUND(400,"Facebook account linked false.", HttpStatus.FOUND),
    GOOGLE_LINK_FOUND(400, "Google account linked false.", HttpStatus.FOUND),
    USER_NOT_AUTHENTICATED(401, "User not authenticated", HttpStatus.UNAUTHORIZED),
    LINKED_ACCOUNT_MISMATCH(400, "linked account mismatch", HttpStatus.BAD_REQUEST),
    INVALID_TOKEN(401, "invalid token", HttpStatus.UNAUTHORIZED),
    UNAUTHORIZED_ACCESS(401, "Access is denied due to invalid credentials", HttpStatus.UNAUTHORIZED),
    TOKEN_NOT_FOUND(404, "Token not found",HttpStatus.NOT_FOUND),
    INVALID_TOKEN_FORMAT(501, "Invalid token format", HttpStatus.SERVICE_UNAVAILABLE),
    SESSION_INVALID(401, "Session is invalid or expired", HttpStatus.INTERNAL_SERVER_ERROR),
    UNKNOWN_ERROR(500, "Unexpected error occurred", HttpStatus.INTERNAL_SERVER_ERROR),
    OAUTH2_CONFIGURATION(414,"OAuth2 configuration is missing information.", HttpStatus.REQUEST_URI_TOO_LONG),
    OAUTH2_REDIRECT_FAILED(410,"An error occurred while generating the redirect URI.", HttpStatus.BAD_REQUEST),
    USER_NOT_FOUND(404, "User Not Found", HttpStatus.NOT_FOUND),
    ROLE_NOT_FOUND(420, "RoleRequest not found", HttpStatus.NOT_FOUND),
    USER_ALREADY_EXISTS(409, "User Already Exists", HttpStatus.CONFLICT),
    USERNAME_INVALID(400, "Username is invalid", HttpStatus.BAD_REQUEST),
    PASSWORD_INVALID(400, "Password is invalid", HttpStatus.BAD_REQUEST),
    PASSWORD_TOO_SHORT(400, "Password is too short", HttpStatus.BAD_REQUEST),
    INVALID_KEY(400, "Invalid Key", HttpStatus.BAD_REQUEST),
    AUTHENTICATION_FAILED(401, "Authentication Failed", HttpStatus.UNAUTHORIZED),
    UNAUTHORIZED(401, "Unauthorized", HttpStatus.UNAUTHORIZED),
    ACCESS_DENIED(403, "Access Denied", HttpStatus.FORBIDDEN),
    PASSWORD_INCORRECT(400, "Password is incorrect", HttpStatus.BAD_REQUEST),
    EMAIL_EXIST(409, "Email has been used!", HttpStatus.CONFLICT),
    INVALID_REFRESH_TOKEN(403, "Invalid refresh token", HttpStatus.FORBIDDEN),
    LOGOUT_BAD_REQUEST(404, "Logout bad request" , HttpStatus.BAD_REQUEST),
    EMAIL_NOT_EXIST(404, "Email not exist", HttpStatus.NOT_FOUND);

    private final int code;
    private final String message;
    private final HttpStatus httpStatus;


}
