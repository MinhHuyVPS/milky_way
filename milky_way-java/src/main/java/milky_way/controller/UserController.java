package milky_way.controller;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.servlet.http.HttpServletRequest;
import milky_way.dto.request.user.UserRequest;
import milky_way.dto.request.user.UserUpdateRequest;
import milky_way.dto.response.user.UserResponse;
import milky_way.dto.response.user.UserUpdateResponse;
import milky_way.service.token.TokenBlacklistService;
import milky_way.service.user.UserService;
import milky_way.utils.config.JwtTokenProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("api/v1/user")
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private TokenBlacklistService tokenBlacklistService;

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('USER') and hasAuthority('USER_READ')")
    public ResponseEntity<UserResponse> getUserById(@PathVariable String id) {
        return ResponseEntity.ok(userService.getById(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('USER') and hasAuthority('USER_WRITE')")
    public ResponseEntity<UserUpdateResponse> updateUser(HttpServletRequest request, @RequestBody UserUpdateRequest userUpdateRequest) {
        String token = tokenBlacklistService.getJwtFromRequest(request);
        String id = jwtTokenProvider.getIdFromJWT(token);
        return ResponseEntity.ok(userService.update(id, userUpdateRequest));
    }
}
