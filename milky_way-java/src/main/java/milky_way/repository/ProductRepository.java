package milky_way.repository;

import milky_way.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface ProductRepository  extends JpaRepository<Product, String> {
    Page<Product> search(String name, Long price, Long stock, LocalDateTime createDate, Pageable pageable);
}
