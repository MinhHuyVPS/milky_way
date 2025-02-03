package milky_way.dto.request.product;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ProductUpdateRequest {
    private String name;
    private String description;
    private Long price;
    private Long stock;
    private String image;
    private LocalDateTime updateDate;
}
