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
    private LocalDateTime create_at;
    private LocalDateTime update_at;
    @ManyToOne
    @JoinColumn(name = "category_id", nullable = false)
    private Category categories;
    @ManyToOne
    @JoinColumn(name = "seller_id", nullable = false)
    private Seller sellers;
    @OneToMany(mappedBy = "products", cascade = CascadeType.ALL)
    private Set<ProductReview> productReviews;
    @OneToMany(mappedBy = "products", cascade = CascadeType.ALL)
    private Set<OrderItem> orderItems;
}
