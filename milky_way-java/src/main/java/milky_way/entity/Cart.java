package milky_way.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Set;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Table(name = "carts")
public class Cart {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    private Long totalPrice;
    private LocalDateTime createDate = LocalDateTime.now();
    private LocalDateTime updateDate;
    @OneToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User users;
    @OneToMany(mappedBy = "carts",cascade = CascadeType.ALL)
    private Set<CartItem> cartItems;
}
