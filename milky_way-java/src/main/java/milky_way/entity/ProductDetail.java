package milky_way.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "product_details")
public class ProductDetail {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    @Column(name = "product_description", columnDefinition = "TEXT")
    private String productDescription;
    private String brand;
    private String image;
    private String dimensions;
    private Double weight;
    private String material;
    private String origin;
    @OneToOne
    @JoinColumn(name = "product_id", nullable = false)
    private Product products;
}
