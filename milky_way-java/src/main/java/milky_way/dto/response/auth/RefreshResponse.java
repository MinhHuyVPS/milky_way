package milky_way.dto.response.auth;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import milky_way.dto.response.session.SessionResponse;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RefreshResponse {
    private String accessToken;
    private SessionResponse sessionResponse;
}
