package milky_way.dto.response.user;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import milky_way.entity.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserUpdateResponse {
    private String username;
    private String email;
    private String firstname;
    private String lastname;
    private String phone;
    private String address;
    private Boolean status;
    private LocalDate birthday;
    private LocalDateTime update_at;
}
