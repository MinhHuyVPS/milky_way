package milky_way.dto.request.role;

import lombok.Builder;
import lombok.Data;

import java.util.Set;

@Data
public class RoleRequest {
    private Set<String> name;
}
