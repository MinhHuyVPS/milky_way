package milky_way.mapper;

import milky_way.dto.response.role.RoleResponse;
import milky_way.entity.Role;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface RoleMapper {
    RoleResponse toRoleResponse(Role role);
}
