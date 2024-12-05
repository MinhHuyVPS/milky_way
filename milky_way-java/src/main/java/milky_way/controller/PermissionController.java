package milky_way.controller;

import milky_way.dto.request.permission.PermissionRequest;
import milky_way.entity.Permission;
import milky_way.service.PermissionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@CrossOrigin(origins = "http://localhost:3000")
@RequestMapping("api/v1/permissions")
@Validated
public class PermissionController {

    @Autowired
    private PermissionService permissionService;

    @PostMapping
    public ResponseEntity<Permission> createPermission(@RequestBody PermissionRequest permissionRequest) {
        Permission permission = permissionService.createPermission(permissionRequest);
        return new ResponseEntity<>(permission, HttpStatus.CREATED);
    }

}
