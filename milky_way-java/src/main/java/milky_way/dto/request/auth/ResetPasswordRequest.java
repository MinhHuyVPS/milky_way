package milky_way.dto.request.auth;

import lombok.Data;

@Data
public class ResetPasswordRequest {
    private String otp;
    private String newPassword;
}
