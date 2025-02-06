package milky_way.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import milky_way.utils.enums.DiscountType;

import java.time.LocalDateTime;
import java.util.Set;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "voucher")
public class Voucher {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    @Column(name = "promotion_name", nullable = false)
    private String promotionName;
    @Column(name = "promotion_code", nullable = false, unique = true)
    private String promotionCode;
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;
    @Enumerated(EnumType.STRING)
    @Column(name = "discount_type", nullable = false)
    private DiscountType discountType;
    private Double discountValue;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    @Column(name = "max_usage", nullable = false)
    private Integer maxUsage;
    @Column(name = "used_count", nullable = false)
    private Integer usedCount;
    @ManyToMany
    @JoinTable(
            name = "promotion_products",
            joinColumns = @JoinColumn(name = "promotion_id"),
            inverseJoinColumns = @JoinColumn(name = "product_id")
    )
    private Set<Product> applicableProducts;
    @ManyToMany
    @JoinTable(
            name = "promotion_users",
            joinColumns = @JoinColumn(name = "promotion_id"),
            inverseJoinColumns = @JoinColumn(name = "user_id")
    )
    private Set<User> applicableUsers;
}
