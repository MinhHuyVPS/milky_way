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
@Table(name = "shops")
public class Shop {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    private String storeName;
    @Column(name = "store_address", columnDefinition = "TEXT")
    private String storeAddress;
    private String taxId;
    private String contactEmail;
    private String contactPhone;
    private LocalDateTime createdDate = LocalDateTime.now();
    private LocalDateTime deleteDate;
    private LocalDateTime updateDate;
    @OneToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User users;
    @OneToMany(mappedBy = "shops", cascade = CascadeType.ALL)
    private Set<Product> products;
    @OneToMany(mappedBy = "shops", cascade = CascadeType.ALL)
    private Set<Category> categories;
}
