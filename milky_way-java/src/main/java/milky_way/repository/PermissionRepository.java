package milky_way.repository;

import milky_way.entity.Permission;
import milky_way.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.Set;

public interface PermissionRepository extends JpaRepository<Permission, String> {
    Optional<Permission> findByName(String name);
    Set<Permission> findByNameIn(Set<String> roleNames);
}
