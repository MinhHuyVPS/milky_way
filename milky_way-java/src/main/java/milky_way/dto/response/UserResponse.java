package milky_way.dto.response;

import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;
@Data
public class UserResponse {
    private String id;
    private String username;
    private String email;
    private String firstname;
    private String lastname;
    private String phone;
    private String address;
    private String status;
    private LocalDate birthday;
    private LocalDateTime create_at;
    private LocalDateTime update_at;
}
