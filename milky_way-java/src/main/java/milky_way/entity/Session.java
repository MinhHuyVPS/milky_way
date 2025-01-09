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
@Table(name = "sessions")
public class Session {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    private LocalDateTime createdDate = LocalDateTime.now();
    private LocalDateTime expiresDate;
    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User users;
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresDate);
    }
}
