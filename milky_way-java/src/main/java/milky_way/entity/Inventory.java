package milky_way.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "inventories")
public class Inventory {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    @OneToOne
    @JoinColumn(name = "product_id", nullable = false)
    private Product products;
    @Column(name = "stock_level", nullable = false)
    private Integer stockLevel;
    @Column(name = "restock_threshold", nullable = false)
    private Integer restockThreshold;
    private LocalDateTime restockDate;
    private LocalDateTime lastUpdated;
    @Column(name = "warehouse_location")
    private String warehouseLocation;
}
