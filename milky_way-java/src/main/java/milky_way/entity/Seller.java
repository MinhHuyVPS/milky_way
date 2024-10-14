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
    private String store_name;
    private String store_address;
    private String tax_id;
    private String contact_email;
    private String contact_phone;
    private LocalDateTime created_at;
    private LocalDateTime update_at;
    @OneToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User users;
    @OneToMany(mappedBy = "sellers", cascade = CascadeType.ALL)
    private Set<Product> products;
}
