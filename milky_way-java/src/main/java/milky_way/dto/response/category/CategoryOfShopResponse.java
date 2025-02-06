package milky_way.dto.response.category;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import milky_way.entity.Category;
import milky_way.entity.Shop;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CategoryOfShopResponse {
    private String name;
    private String description;
    private Shop shop;
    private Category parent;
}
