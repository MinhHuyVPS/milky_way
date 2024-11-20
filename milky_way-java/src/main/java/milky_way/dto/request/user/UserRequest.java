package milky_way.dto.request.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import java.time.LocalDate;
@Data
public class UserRequest {
    @NotBlank(message = "username.not.blank")
    private String username;
    @Email
    private String email;
    private String password;
    private String firstname;
    private String lastname;
    private String phone;
    private String address;
    private String status;
    private LocalDate birthday;
}
