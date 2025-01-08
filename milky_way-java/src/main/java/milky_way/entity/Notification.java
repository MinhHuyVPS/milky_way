package milky_way.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import milky_way.utils.NotificationStatus;
import milky_way.utils.NotificationType;

import java.time.LocalDateTime;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "notifications")
public class Notification {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    @Enumerated(EnumType.STRING)
    @Column(name = "notification_type", nullable = false)
    private NotificationType notificationType;
    @Column(name = "message", columnDefinition = "TEXT")
    private String message;
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private NotificationStatus status;
    private LocalDateTime createdAt;
}
