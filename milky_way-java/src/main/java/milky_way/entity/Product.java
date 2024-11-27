package milky_way.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Set;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Entity
@Builder
@Table(name = "products")
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    private String name;
    private String description;
    private Long price;
    private Long stock;
    private LocalDateTime createAt;
    private LocalDateTime updateAt;
    @ManyToOne
    @JoinColumn(name = "category_id", nullable = false)
    private Category categories;
    @ManyToOne
    @JoinColumn(name = "seller_id", nullable = false)
    private Seller sellers;
    @OneToMany(mappedBy = "products", cascade = CascadeType.ALL)
    private Set<ProductReview> productReviews;
    @ManyToMany
    @JoinTable(
            name = "products_order_items",
            joinColumns = @JoinColumn(name = "product_id"),
            inverseJoinColumns = @JoinColumn(name = "order_item_id")
    )
    private Set<OrderItem> orderItems;
    @OneToMany(mappedBy = "products", cascade = CascadeType.ALL)
    private Set<CartItem> cartItems;
}
