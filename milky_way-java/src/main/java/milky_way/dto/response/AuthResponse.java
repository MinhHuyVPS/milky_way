package milky_way.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AuthResponse {
    private String googleId;
    private String facebookId;
    private String accessToken;
    private String refreshToken;
    private SessionResponse sessionResponse;
}
