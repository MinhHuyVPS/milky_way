package milky_way.repository;

import milky_way.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
@Repository
public interface UserRepository extends JpaRepository<User, String> {
    @Query("SELECT u FROM User u WHERE u.email = :email OR u.googleEmail = :email OR u.facebookEmail = :email")
    Optional<User> findByAnyEmail(@Param("email") String email);
    Optional<User> findByEmail(String email);
    Optional<User> findByGoogleEmail(String email);
    boolean existsByEmail(String email);
    Optional<User> findUserWithRolesByEmail(String email);
    boolean existsByGoogleEmail(String googleEmail);
    boolean existsByFacebookEmail(String facebookEmail);
}
