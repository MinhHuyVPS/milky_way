package milky_way.service.category;

import milky_way.dto.request.category.CategoryRequest;
import milky_way.dto.response.category.CategoryResponse;
import milky_way.entity.Category;
import milky_way.exception.AppException;
import milky_way.exception.ErrorCode;
import milky_way.mapper.CategoryMapper;
import milky_way.repository.CategoryRepository;
import milky_way.service.config.RedisService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class CategoryService {

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private RedisService redisService;

    @Autowired
    private CategoryMapper categoryMapper;

    public CategoryResponse create(CategoryRequest categoryRequest){
        Category category = categoryMapper.toCategory(categoryRequest);
        category.setName(categoryRequest.getName());
        category.setDescription(categoryRequest.getDescription());
        Category saveCategory = categoryRepository.save(category);
        return categoryMapper.useCategory(saveCategory);
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
