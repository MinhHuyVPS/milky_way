package milky_way.dto.request.user;

import lombok.Data;
import java.time.LocalDate;
@Data
public class UserRequest {
    private String username;
    private String email;
    private String password;
    private String firstname;
    private String lastname;
    private String phone;
    private String address;
    private String status;
    private LocalDate birthday;
}
