package milky_way.dto.request.product;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import milky_way.entity.Category;
import milky_way.entity.Shop;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductOfShopRequest {
    private String name;
    private String description;
    private Long price;
    private Long stock;
    private String image;
    private Shop shop;
    private Category category;
}
