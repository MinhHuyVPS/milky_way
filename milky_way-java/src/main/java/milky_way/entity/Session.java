package milky_way.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Table(name = "session")
public class Session {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;
    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }
}
