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
@Table(name = "product_reviews")
public class ProductReview {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    private Integer rating;
    @Column(name = "review", columnDefinition = "TEXT")
    private String review;
    private LocalDateTime createAt;
    @ManyToOne
    @JoinColumn(name = "product_id", nullable = false)
    private Product products;
}
