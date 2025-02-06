package milky_way.service.category;

import milky_way.dto.request.category.CategoryOfShopRequest;
import milky_way.dto.request.category.CategoryRequest;
import milky_way.dto.response.category.CategoryOfShopResponse;
import milky_way.dto.response.category.CategoryResponse;
import milky_way.entity.Category;
import milky_way.entity.Shop;
import milky_way.exception.AppException;
import milky_way.exception.ErrorCode;
import milky_way.mapper.CategoryMapper;
import milky_way.repository.CategoryRepository;
import milky_way.repository.ShopRepository;
import milky_way.service.config.RedisService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.EOFException;

@Service
public class CategoryService {

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private RedisService redisService;

    @Autowired
    private CategoryMapper categoryMapper;

    @Autowired
    private ShopRepository shopRepository;

    public CategoryResponse create(CategoryRequest categoryRequest){
        Category category = categoryMapper.toCategory(categoryRequest);
        category.setName(categoryRequest.getName());
        category.setDescription(categoryRequest.getDescription());
        Category saveCategory = categoryRepository.save(category);
        return categoryMapper.useCategory(saveCategory);
    }

    public CategoryOfShopResponse createCategoriesShop(CategoryOfShopRequest categoryOfShopRequest){
        boolean exists = categoryRepository.existsByNameAndShop(categoryOfShopRequest.getName(), categoryOfShopRequest.getShop().getId());
        if (exists) {
            throw new AppException(ErrorCode.CATEGORY_ALREADY_EXISTS);
        }
        Shop shop = shopRepository.findById(categoryOfShopRequest.getShop().getId())
                .orElseThrow(() -> new AppException(ErrorCode.CATEGORY_NOT_FOUND));
        Category parent = null;
        if (categoryOfShopRequest.getParent() != null) {
            parent = categoryRepository.findById(categoryOfShopRequest.getParent().getId())
                    .orElseThrow(() -> new AppException(ErrorCode.CATEGORY_NOT_FOUND));
            if (parent.getShops() != null && !parent.getShops().getId().equals(shop.getId())) {
                throw new AppException(ErrorCode.CATEGORY_NOT_FOUND);
            }
        }
        Category category = categoryMapper.toCategoryShop(categoryOfShopRequest);
        category.setName(categoryOfShopRequest.getName());
        category.setShops(shop);
        category.setParent(parent);
        Category saveCategory = categoryRepository.save(category);
        return categoryMapper.useCategoryShop(saveCategory);
    }

    public CategoryResponse getById(String id){
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.CATEGORY_NOT_FOUND));;
        return categoryMapper.useCategory(category);
    }

    public CategoryResponse update(String id, CategoryRequest categoryRequest){
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.CATEGORY_NOT_FOUND));
        category.setName(categoryRequest.getName());
        category.setDescription(categoryRequest.getDescription());
        Category categorySave = categoryRepository.save(category);
        return categoryMapper.useCategory(categorySave);
    }

    public void remove(String id){
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.CATEGORY_NOT_FOUND));
        categoryRepository.delete(category);
    }
}
