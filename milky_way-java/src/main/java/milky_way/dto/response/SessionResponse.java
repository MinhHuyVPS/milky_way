package milky_way.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SessionResponse {

    private String id;
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;
}
