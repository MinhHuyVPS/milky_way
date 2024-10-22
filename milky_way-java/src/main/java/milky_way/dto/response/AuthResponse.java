package milky_way.dto.response;

import lombok.Data;

@Data
public class AuthResponse {

    private String accessToken;
    private String refreshToken;

}
