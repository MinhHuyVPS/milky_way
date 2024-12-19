package milky_way.mapper;

import milky_way.dto.response.permission.PermissionResponse;
import milky_way.entity.Permission;
import org.mapstruct.Mapper;

import java.util.Set;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring")
public interface PermissionMapper {
    PermissionResponse toPermission(Permission permission);
    default Set<PermissionResponse> toPermissionSet(Set<Permission> permission){
        return permission.stream()
                .map(this::toPermission)
                .collect(Collectors.toSet());
    };
}
