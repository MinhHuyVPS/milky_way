package milky_way.repository;

import milky_way.entity.Token;
import milky_way.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TokenRepository extends JpaRepository<Token, String> {
    List<Token> findByUsers(User user);
}
