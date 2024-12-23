package milky_way.dto.request.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import milky_way.utils.validation.ValidPassword;

import java.time.LocalDate;
@Data
public class UserRequest {
    @NotBlank(message = "{username.not.blank}")
    private String username;
    @Email(message = "{email.invalid}")
    private String email;
    @ValidPassword
    private String password;
    private String firstname;
    private String lastname;
    private String phone;
    private String address;
    private Boolean status;
    private LocalDate birthday;
}
