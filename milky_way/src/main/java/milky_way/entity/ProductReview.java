package milky_way.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Set;

@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Data
@Table(name = "order_reviews")
public class ProductReview {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    private Integer rating;
    private String review;
    private LocalDateTime create_at;
    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User users;
    @ManyToOne
    @JoinColumn(name = "product_id", nullable = false)
    private Product products;
}
