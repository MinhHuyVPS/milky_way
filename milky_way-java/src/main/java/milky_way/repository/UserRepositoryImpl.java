package milky_way.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import milky_way.entity.User;
import milky_way.repository.intefaceRepository.UserRepositoryCustom;

import java.util.Optional;

public class UserRepositoryImpl implements UserRepositoryCustom {

    @PersistenceContext
    private EntityManager entityManager;

    public Optional<User> findUserWithRolesByEmail(String email){
        String query =  "SELECT u FROM User u LEFT JOIN FETCH u.roles WHERE u.email = :email";
        TypedQuery<User> typedQuery = entityManager.createQuery(query, User.class);
        typedQuery.setParameter("email", email);
        return Optional.ofNullable(typedQuery.getSingleResult());
    }
}
