package milky_way.dto.request.category;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import milky_way.entity.Category;
import milky_way.entity.Shop;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CategoryOfShopRequest {
    @NotBlank(message = "{name.not.blank}")
    private String name;
    private String description;
    private Shop shop;
    private Category parent;
}
