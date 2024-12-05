package milky_way.service;

import milky_way.dto.request.permission.PermissionRequest;
import milky_way.entity.Permission;
import milky_way.exception.AppException;
import milky_way.exception.ErrorCode;
import milky_way.repository.PermissionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class PermissionService {

    @Autowired
    private PermissionRepository permissionRepository;

    public Permission createPermission(PermissionRequest permissionRequest){
        if(permissionRepository.findByName(permissionRequest.getName()).isPresent()){
            throw new AppException(ErrorCode.PERMISSION__EXIST);
        }
        Permission permission = Permission.builder()
                .name(permissionRequest.getName())
                .description(permissionRequest.getDescription())
                .build();
        return permissionRepository.save(permission);
    }
}
