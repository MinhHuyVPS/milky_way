package milky_way.mapper;

import milky_way.dto.request.product.ProductOfShopRequest;
import milky_way.dto.request.product.ProductRequest;
import milky_way.dto.response.product.ProductOfShopResponse;
import milky_way.dto.response.product.ProductResponse;
import milky_way.entity.Product;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ProductMapper {
    Product toProduct(ProductRequest productRequest);
    ProductResponse toProductResponse(Product product);
    Product toProductShop(ProductOfShopRequest productOfShopRequest);
    ProductOfShopResponse useProduct(Product product);
}
