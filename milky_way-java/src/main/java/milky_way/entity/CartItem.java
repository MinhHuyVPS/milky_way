package milky_way.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
@Table(name = "cart_items")
public class CartItem {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    private Long quantity;
    private Long unitPrice;
    @ManyToOne
    @JoinColumn(name = "cart_id", nullable = false)
    private Cart carts;
    @ManyToOne
    @JoinColumn(name = "product_id", nullable = false)
    private Product products;
}
