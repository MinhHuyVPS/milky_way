package milky_way.repository.intefaceRepository;

import milky_way.entity.User;

import java.util.Optional;

public interface UserRepositoryCustom {
    Optional<User> findUserWithRolesByEmail(String email);

}
