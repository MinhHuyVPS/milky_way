package milky_way.service;

import jakarta.annotation.PostConstruct;
import jakarta.transaction.Transactional;
import milky_way.dto.request.permission.PermissionRequest;
import milky_way.dto.response.role.RoleResponse;
import milky_way.entity.Permission;
import milky_way.entity.Role;
import milky_way.entity.User;
import milky_way.exception.AppException;
import milky_way.exception.ErrorCode;
import milky_way.mapper.RoleMapper;
import milky_way.repository.PermissionRepository;
import milky_way.repository.RoleRepository;
import milky_way.utils.Permissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class PermissionService {

    @Autowired
    private PermissionRepository permissionRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private RoleMapper roleMapper;

    @PostConstruct
    public void initPermissions() {
        try {
            for (Permissions permissions : Permissions.values()) {
                if (permissionRepository.findByName(permissions.getValue()).isEmpty()) {
                    Permission permission = Permission.builder()
                            .name(permissions.getValue())
                            .description(permissions.getValue())
                            .build();
                    permissionRepository.save(permission);
                }
            }
            Role adminRole = roleRepository.findByName("ADMIN")
                    .orElseGet(() -> {
                        Role newRole = new Role();
                        newRole.setName("ADMIN");
                        return roleRepository.save(newRole);
                    });
            if (adminRole.getPermissions() == null) {
                adminRole.setPermissions(new HashSet<>());
            }
            List<Permission> permissionsList = permissionRepository.findAll();
            for (Permission permission : permissionsList) {
                if (!adminRole.getPermissions().contains(permission)) {
                    adminRole.getPermissions().add(permission);
                }
            }
            roleRepository.save(adminRole);
        } catch (Exception e) {
//            System.err.println("Lỗi khi khởi tạo quyền và role: " + e.getMessage());
            e.printStackTrace();
        }
    }


    @Transactional
    public RoleResponse addPermissionsToRole(PermissionRequest permissionRequest){
        if (permissionRequest.getId() == null || permissionRequest.getId().isEmpty()) {
            throw new AppException(ErrorCode.INVALID_REQUEST);
        }
        Role role = roleRepository.findById(permissionRequest.getId())
                .orElseThrow(() -> new AppException(ErrorCode.ROLE_NOT_FOUND));
        Set<Permission> permissions = permissionRepository.findByNameIn(permissionRequest.getName());
        if (permissions.isEmpty()){
            throw new AppException(ErrorCode.PERMISSION_NOT_FOUND);
        }
        role.getPermissions().addAll(permissions);
        roleRepository.save(role);
        return roleMapper.toRoleResponse(role);
    }

    @Transactional
    public RoleResponse updatePermissionsForRole(PermissionRequest permissionRequest) {
        if (permissionRequest.getId() == null || permissionRequest.getId().isEmpty()) {
            throw new AppException(ErrorCode.INVALID_REQUEST);
        }
        Role role = roleRepository.findById(permissionRequest.getId())
                .orElseThrow(() -> new AppException(ErrorCode.ROLE_NOT_FOUND));
        Set<Permission> permissions = permissionRepository.findByNameIn(permissionRequest.getName());
        if(permissions.isEmpty()){
            throw new AppException(ErrorCode.PERMISSION_NOT_FOUND);
        }
        role.getPermissions().clear();
        role.getPermissions().addAll(permissions);
        roleRepository.save(role);
        return roleMapper.toRoleResponse(role);
    }
}
