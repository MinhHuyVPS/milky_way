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
@AllArgsConstructor
@NoArgsConstructor
@Data
@Table(name = "sellers")
public class Seller {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    private String storeName;
    private String storeAddress;
    private String taxId;
    private String contactEmail;
    private String contactPhone;
    private LocalDateTime createdAt;
    private LocalDateTime updateAt;
    @OneToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User users;
    @OneToMany(mappedBy = "sellers", cascade = CascadeType.ALL)
    private Set<Product> products;
}
