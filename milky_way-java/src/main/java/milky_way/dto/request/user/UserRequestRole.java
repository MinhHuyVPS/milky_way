package milky_way.dto.request.user;

import lombok.Data;

import java.util.Set;

@Data
public class UserRequestRole {
    private String id;
    private Set<String> name;
}
