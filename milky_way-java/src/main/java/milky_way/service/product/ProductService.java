package milky_way.service.product;

import milky_way.dto.request.product.ProductOfShopRequest;
import milky_way.dto.request.product.ProductRequest;
import milky_way.dto.response.product.ProductOfShopResponse;
import milky_way.dto.response.product.ProductResponse;
import milky_way.entity.Category;
import milky_way.entity.Product;
import milky_way.entity.Shop;
import milky_way.exception.AppException;
import milky_way.exception.ErrorCode;
import milky_way.mapper.ProductMapper;
import milky_way.repository.CategoryRepository;
import milky_way.repository.ProductRepository;
import milky_way.repository.ShopRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.lang.module.ResolutionException;
import java.time.LocalDateTime;

@Service
public class ProductService {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ProductMapper productMapper;

    @Autowired
    private ShopRepository shopRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    public Page<Product> search(String name, Long price, Long stock, LocalDateTime createDate, Pageable pageable){
        return productRepository.search(name, price, stock, createDate, pageable);
    }

    public Product getById(String id){
        return productRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_FOUND));
    }

    public ProductResponse create(ProductRequest productRequest){
        Product product = productMapper.toProduct(productRequest);
        product.setName(productRequest.getName());
        product.setDescription(productRequest.getDescription());
        product.setPrice(productRequest.getPrice());
        product.setStock(productRequest.getStock());
        product.setImage(productRequest.getImage());
        Product saveProduct = productRepository.save(product);
        return productMapper.toProductResponse(saveProduct);
    }

    public ProductOfShopResponse createShopOfProduct(ProductOfShopRequest productOfShopRequest){
        Shop shop = shopRepository.findById(productOfShopRequest.getShop().getId())
                .orElseThrow(() -> new AppException(ErrorCode.SHOP_NOT_FOUND));
        Category category = categoryRepository.findById(productOfShopRequest.getCategory().getId())
                .orElseThrow(() -> new AppException(ErrorCode.CATEGORY_NOT_FOUND));
        if(category.getShops() != null && !category.getShops().getId().equals(productOfShopRequest.getShop().getId())){
            throw new AppException(ErrorCode.NOT_SHOP_AND_CATEGORY);
        }
        Product product = productMapper.toProductShop(productOfShopRequest);
        product.setName(productOfShopRequest.getName());
        product.setDescription(productOfShopRequest.getDescription());
        product.setPrice(productOfShopRequest.getPrice());
        product.setStock(productOfShopRequest.getStock());
        product.setShops(productOfShopRequest.getShop());
        product.setCategories(productOfShopRequest.getCategory());
        Product saveProduct = productRepository.save(product);
        return productMapper.useProduct(saveProduct);
    }
}
