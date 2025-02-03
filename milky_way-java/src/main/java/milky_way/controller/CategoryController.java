package milky_way.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import milky_way.dto.request.category.CategoryRequest;
import milky_way.dto.request.user.UserRequest;
import milky_way.dto.request.user.UserUpdateRequest;
import milky_way.dto.response.category.CategoryResponse;
import milky_way.dto.response.user.UserResponse;
import milky_way.dto.response.user.UserUpdateResponse;
import milky_way.exception.AppException;
import milky_way.exception.ErrorCode;
import milky_way.service.category.CategoryService;
import milky_way.service.token.TokenBlacklistService;
import milky_way.utils.config.JwtTokenProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("api/v1/categories")
public class CategoryController {

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private TokenBlacklistService tokenBlacklistService;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN') and hasAuthority('ADMIN_READ')")
    public ResponseEntity<CategoryResponse> getUserById(@PathVariable String id) {
        return ResponseEntity.ok(categoryService.getById(id));
    }

    @PostMapping("/categories")
    @PreAuthorize("hasRole('ADMIN') and hasAuthority('ADMIN_WRITE')")
    public ResponseEntity<?> create(@Valid @RequestBody CategoryRequest categoryRequest) {
        try {
            CategoryResponse categoryResponse = categoryService.create(categoryRequest);
            return ResponseEntity.ok(categoryResponse);
        } catch (AppException e) {
            return ResponseEntity.status(e.getErrorCode().getHttpStatus())
                    .body(e.getErrorCode().getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(ErrorCode.UNKNOWN_ERROR.getHttpStatus())
                    .body(ErrorCode.UNKNOWN_ERROR.getMessage());
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') and hasAuthority('ADMIN_WRITE')")
    public ResponseEntity<CategoryResponse> update(HttpServletRequest request, @RequestBody CategoryRequest categoryRequest) {
        String token = tokenBlacklistService.getJwtFromRequest(request);
        String id = jwtTokenProvider.getIdFromJWT(token);
        return ResponseEntity.ok(categoryService.update(id, categoryRequest));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') and hasAuthority('ADMIN_DELETE')")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        categoryService.remove(id);
        return ResponseEntity.noContent().build();
    }
}
