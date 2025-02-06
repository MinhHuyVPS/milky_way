package milky_way.repository;

import milky_way.entity.Category;
import milky_way.entity.Shop;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryRepository extends JpaRepository<Category, String> {
    Boolean existsByNameAndShop(String name, String shop);
}
