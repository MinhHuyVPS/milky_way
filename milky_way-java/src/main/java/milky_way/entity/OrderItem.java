package milky_way.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

@NoArgsConstructor
@Entity
@AllArgsConstructor
@Builder
@Data
@Table(name = "order_items")
public class OrderItem {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    private Long quantity;
    private Long unit_price;
    private Long total_price;
    @ManyToOne
    @JoinColumn(name = "order_id", nullable = false)
    private Order orders;
}
