package milky_way.mapper;

import milky_way.dto.request.category.CategoryOfShopRequest;
import milky_way.dto.request.category.CategoryRequest;
import milky_way.dto.response.category.CategoryOfShopResponse;
import milky_way.dto.response.category.CategoryResponse;
import milky_way.entity.Category;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface CategoryMapper {
    Category toCategory(CategoryRequest categoryRequest);
    CategoryResponse useCategory(Category category);
    Category toCategoryShop(CategoryOfShopRequest category);
    CategoryOfShopResponse useCategoryShop(Category category);
}
