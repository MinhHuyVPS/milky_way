package milky_way.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Set;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "orders")
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    private String status;
    private Long total_price;
    private String shipping_address;
    private String payment_status;
    private LocalDateTime create_at;
    private LocalDateTime update_at;
    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User users;
    @OneToMany(mappedBy = "orders", cascade = CascadeType.ALL)
    private Set<OrderItem> orderItems;
    @OneToMany(mappedBy = "orders", cascade = CascadeType.ALL)
    private Set<Payment> payments;
}
