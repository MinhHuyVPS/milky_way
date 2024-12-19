package milky_way.repository;

import io.lettuce.core.dynamic.annotation.Param;
import milky_way.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.Set;

@Repository
public interface RoleRepository extends JpaRepository<Role, String> {
    Optional<Role> findByName(String name);
    Set<Role> findByNameIn(Set<String> roleNames);
    @Query(value = "SELECT p.id, p.name FROM permissions p " +
            "JOIN roles_permissions rp ON rp.permission_id = p.id " +
            "JOIN roles r ON r.id = rp.role_id " +
            "WHERE r.id IN :roleIds", nativeQuery = true)
    Set<Object[]> findPermissionsByRolesIn(@Param("roleIds") Set<String> roleIds);

}
