package milky_way.repository;

import milky_way.entity.Session;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SessionRepository extends JpaRepository<Session, String> {
    Optional<Session> findByUsersEmail(String email);
    List<Session> findAllByUsersEmail(String email);
    @Query("SELECT s FROM Session s WHERE s.users.id = :userId")
    Optional<Session> findByUserId(String userId);
}