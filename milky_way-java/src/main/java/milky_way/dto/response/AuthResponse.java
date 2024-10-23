package milky_way.dto.response;

import lombok.Data;
import milky_way.entity.Session;

@Data
public class AuthResponse {

    private String accessToken;
    private String refreshToken;
    private SessionResponse sessionResponse;

}
