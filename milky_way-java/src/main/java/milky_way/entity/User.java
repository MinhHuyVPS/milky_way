package milky_way.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;

@Entity
@Table(name = "users")
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Data
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    private String username;
    private String email;
    private String password;
    private String firstname;
    private String lastname;
    private String phone;
    private String address;
    private String status;
    private LocalDate birthday;
    private LocalDateTime delete_at;
    private LocalDateTime create_at;
    private LocalDateTime update_at;
    @ManyToMany
    @JoinTable(
            name = "users_roles",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    private Set<Role> roles;
    @OneToMany(mappedBy = "users", cascade = CascadeType.ALL)
    private Set<Order> orders;
    @ManyToMany
    @JoinTable(
            name = "users_product_reviews",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "product_review_id")
    )
    private Set<ProductReview> productReviews;
    @OneToOne(mappedBy = "users", cascade = CascadeType.ALL)
    private Cart cart;
    @OneToOne(mappedBy = "users", cascade = CascadeType.ALL)
    private Seller sellers;
}
