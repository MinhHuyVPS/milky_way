package milky_way.repository;

import milky_way.entity.User;
import milky_way.repository.intefaceRepository.UserRepositoryCustom;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, String>, UserRepositoryCustom {
    Optional<User> findByEmail(String email);
}
