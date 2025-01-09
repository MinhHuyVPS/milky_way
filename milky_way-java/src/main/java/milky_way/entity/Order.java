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
    private boolean status = true;
    private Long totalPrice;
    @Column(name = "shipping_address", columnDefinition = "TEXT")
    private String shippingAddress;
    private String paymentStatus;
    private LocalDateTime createDate = LocalDateTime.now();
    private LocalDateTime updateDate;
    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User users;
    @OneToMany(mappedBy = "orders", cascade = CascadeType.ALL)
    private Set<OrderItem> orderItems;
    @OneToMany(mappedBy = "orders", cascade = CascadeType.ALL)
    private Set<Payment> payments;
    @OneToMany(mappedBy = "orders", cascade = CascadeType.ALL)
    private Set<Ship> ships;
}
