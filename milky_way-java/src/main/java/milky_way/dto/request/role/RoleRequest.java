package milky_way.dto.request.role;

import lombok.Builder;
import lombok.Data;

import java.util.Set;

@Data
public class RoleRequest {
    private String id;
    private Set<String> name;
}
