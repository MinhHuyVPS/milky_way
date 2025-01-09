package milky_way.dto.request.user;

import lombok.Data;
import milky_way.entity.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;

@Data
public class UserUpdateRequest {
    private String username;
    private String email;
    private String firstname;
    private String lastname;
    private String phone;
    private String address;
    private LocalDate birthday;
}
