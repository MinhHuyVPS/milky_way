package milky_way.dto.request.user;

import lombok.Data;

@Data
public class UserRequestLogin {
    private String email;
    private String password;
}
