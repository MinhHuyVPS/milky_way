package milky_way.controller;

import milky_way.dto.request.role.RoleRequestPermission;
import milky_way.dto.request.user.ChangeStatusRequest;
import milky_way.dto.request.user.UserRequestRole;
import milky_way.dto.response.role.RoleResponse;
import milky_way.dto.response.user.UserChangeStatusResponse;
import milky_way.dto.response.user.UserResponse;
import milky_way.exception.AppException;
import milky_way.exception.ErrorCode;
import milky_way.service.permission.PermissionService;
import milky_way.service.role.RoleService;
import milky_way.service.user.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@CrossOrigin(origins = "http://localhost:3000")
@RequestMapping("api/v1/admin")
@Validated
public class AdminController {

    @Autowired
    private UserService userService;

    @Autowired
    private RoleService roleService;

    @Autowired
    private PermissionService permissionService;

    @PostMapping("/add-roles")
    @PreAuthorize("hasRole('ADMIN') and hasAuthority('ADMIN_WRITE')")
    public ResponseEntity<?> addRoles(@RequestBody UserRequestRole roleRequest){
        UserResponse response = roleService.addRolesToUser(roleRequest);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/update-roles")
    @PreAuthorize("hasRole('ADMIN') and hasAnyAuthority('ADMIN_WRITE', 'ADMIN_DELETE')")
    public ResponseEntity<?> updateRoles(@RequestBody UserRequestRole roleRequest){
        UserResponse response = roleService.updateRolesForUser(roleRequest);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/add-permissions")
    @PreAuthorize("hasRole('ADMIN') and hasAuthority('ADMIN_WRITE')")
    public ResponseEntity<?> addPermissions(@RequestBody RoleRequestPermission permissionRequest){
        RoleResponse response = permissionService.addPermissionsToRole(permissionRequest);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/update-permissions")
    @PreAuthorize("hasRole('ADMIN') and hasAnyAuthority('ADMIN_WRITE', 'ADMIN_DELETE')")
    public ResponseEntity<?> updatePermissions(@RequestBody RoleRequestPermission permissionRequest){
        RoleResponse response = permissionService.updatePermissionsForRole(permissionRequest);
        return ResponseEntity.ok(response);
    }

    @PutMapping("change-status")
    @PreAuthorize("hasRole('ADMIN') and hasAuthority('ADMIN_WRITE')")
    public ResponseEntity<UserChangeStatusResponse> changeStatus(@RequestBody ChangeStatusRequest changeStatusRequest) {
        try{
            UserChangeStatusResponse response = userService.changeUserStatus(changeStatusRequest);
            return ResponseEntity.ok(response);
        }catch (AppException e){
         throw new AppException(ErrorCode.ACCESS_DENIED);
        }
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN') and hasAuthority('ADMIN_READ')")
    public ResponseEntity<List<UserResponse>> getAllUsers(){
        return ResponseEntity.ok(userService.getAll());
    }

    @DeleteMapping("/{id}/remove")
    @PreAuthorize("hasRole('ADMIN') and hasAuthority('ADMIN_DELETE')")
    public ResponseEntity<Void> deleteUser(@PathVariable String id) {
        userService.deleted(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') and hasAuthority('ADMIN_DELETE')")
    public ResponseEntity<Void> deleteAddFieldDeletedAtUser(@PathVariable String id) {
        userService.deleteAddFieldDeletedAtUser(id);
        return ResponseEntity.noContent().build();
    }
}
