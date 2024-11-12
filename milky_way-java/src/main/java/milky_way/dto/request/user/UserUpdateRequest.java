package milky_way.dto.request.user;

import lombok.Data;
import milky_way.entity.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;

@Data
public class UserUpdateRequest {
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
    private Set<Role> roles;
    private Set<Order> orders;
    private Set<ProductReview> productReviews;
    private Cart cart;
    private Seller sellers;
}
