package milky_way.service.product;

import milky_way.entity.Product;
import milky_way.exception.AppException;
import milky_way.exception.ErrorCode;
import milky_way.repository.ProductRepository;
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

    public Page<Product> search(String name, Long price, Long stock, LocalDateTime createDate, Pageable pageable){
        return productRepository.search(name, price, stock, createDate, pageable);
    }

    public Product getById(String id){
        return productRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_FOUND));
    }


}
