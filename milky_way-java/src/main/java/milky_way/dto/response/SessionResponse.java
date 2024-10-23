package milky_way.dto.response;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class SessionResponse {

    private String id;
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;

    public SessionResponse(String id, LocalDateTime createdAt, LocalDateTime expiresAt) {
        this.id = id;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
    }
}
