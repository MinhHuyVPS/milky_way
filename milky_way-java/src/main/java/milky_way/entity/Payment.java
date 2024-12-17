package milky_way.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Data
@Table(name = "payments")
public class Payment {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    private String paymentMethod;
    private String paymentStatus;
    private LocalDateTime createAt = LocalDateTime.now();
    @ManyToOne
    @JoinColumn(name = "order_id",nullable = false)
    private Order orders;
}
