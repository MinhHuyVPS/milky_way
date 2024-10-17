package milky_way.exception;

import lombok.*;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ErrorCode {
    USER_NOT_FOUND(404, "User Not Found", HttpStatus.NOT_FOUND),
    USER_ALREADY_EXISTS(409, "User Already Exists", HttpStatus.CONFLICT),
    USERNAME_INVALID(400, "Username is invalid", HttpStatus.BAD_REQUEST),
    PASSWORD_INVALID(400, "Password is invalid", HttpStatus.BAD_REQUEST),
    PASSWORD_TOO_SHORT(400, "Password is too short", HttpStatus.BAD_REQUEST),
    INVALID_KEY(400, "Invalid Key", HttpStatus.BAD_REQUEST),
    AUTHENTICATION_FAILED(401, "Authentication Failed", HttpStatus.UNAUTHORIZED),
    UNAUTHORIZED(401, "Unauthorized", HttpStatus.UNAUTHORIZED),
    ACCESS_DENIED(403, "Access Denied", HttpStatus.FORBIDDEN),
    PASSWORD_INCORRECT(406, "password is incorrect", HttpStatus.BAD_REQUEST),
    EMAIL_DOSE_NOT_EXIST(405, "email dose not exist", HttpStatus.NOT_FOUND);
    private final int code;
    private final String message;
    private final HttpStatus httpStatus;


}
