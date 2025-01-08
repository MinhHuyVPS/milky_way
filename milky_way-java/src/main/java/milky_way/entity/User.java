package milky_way.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
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
    @Column(nullable = false, unique = true)
    private String email;
    private String googleEmail;
    @Column(nullable = false)
    private String password;
    private String firstname;
    private String lastname;
    private String phone;
    @Column(name = "avatar", columnDefinition = "TEXT")
    private String avatar;
    private String googleId;
    private String facebookId;
    private String facebookEmail;
    private String facebookName;
    @Column(name = "address", columnDefinition = "TEXT")
    private String address;
    private Boolean status;
    private LocalDate birthday;
    private LocalDateTime deleteAt;
    @Column(updatable = false)
    private LocalDateTime createAt = LocalDateTime.now();
    private LocalDateTime updateAt;
    @PreUpdate
    protected void onUpdate() {
        updateAt = LocalDateTime.now();
    }
    @ManyToMany(fetch = FetchType.EAGER)
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
    @OneToMany(mappedBy = "users")
    private Set<Session> sessions;
    @OneToMany(mappedBy = "users")
    private Set<Token> tokens;
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return roles.stream()
                .flatMap(role -> role.getPermissions().stream())
                .map(permission -> new SimpleGrantedAuthority(permission.getName()))
                .distinct()
                .toList();
    }
}
