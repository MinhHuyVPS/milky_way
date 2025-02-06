package milky_way.dto.request.product;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ProductRequest {
    @NotBlank(message = "{name.not.blank}")
    private String name;
    private String description;
    private Long price;
    private Long stock;
    private String image;
}
