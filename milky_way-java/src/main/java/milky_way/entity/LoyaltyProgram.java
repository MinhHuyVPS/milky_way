package milky_way.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import milky_way.utils.enums.MembershipLevel;

import java.time.LocalDateTime;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "loyalty_programs")
public class LoyaltyProgram {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    @OneToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User users;
    @Column(name = "points", nullable = false)
    private Integer points;
    @Enumerated(EnumType.STRING)
    @Column(name = "membership_level", nullable = false)
    private MembershipLevel membershipLevel;
    private LocalDateTime lastUpdated;
    private LocalDateTime pointsExpirationDate;
}
