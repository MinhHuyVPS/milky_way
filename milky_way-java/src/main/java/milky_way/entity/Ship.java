package milky_way.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import milky_way.utils.enums.ShippingStatus;

import java.time.LocalDateTime;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "ships")
public class Ship {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    @ManyToOne
    @JoinColumn(name = "order_id", nullable = false)
    private Order orders;
    @Column(name = "shipping_method", nullable = false)
    private String shippingMethod;
    @Column(name = "tracking_number", nullable = false, unique = true)
    private String trackingNumber;
    @Enumerated(EnumType.STRING)
    @Column(name = "shipping_status", nullable = false)
    private ShippingStatus shippingStatus;
    private LocalDateTime shippingDate;
    private LocalDateTime estimatedDeliveryDate;
    private LocalDateTime actualDeliveryDate;
    private Double shippingCost;
    @Column(name = "carrier", nullable = false)
    private String carrier;
}
