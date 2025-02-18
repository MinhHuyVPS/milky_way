package milky_way.utils;

import milky_way.entity.Product;
import org.springframework.data.domain.Pageable;
import jakarta. persistence. criteria. Order;
import org.springframework.data.jpa.domain.Specification;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ProductSpecification {
    public static Specification<Product> hasName(String name) {
        return (root, query, criteriaBuilder) -> criteriaBuilder.like(root.get("name"), "%" + name + "%");
    }

    public static Specification<Product> hasPrice(Long price) {
        return (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("price"), price);
    }

    public static Specification<Product> hasStock(Long stock) {
        return (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("stock"), stock);
    }

    public static Specification<Product> hasCreateDate(LocalDateTime createDate) {
        return (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("createDate"), createDate);
    }

    public static Specification<Product> applySorting(Specification<Product> spec, Pageable pageable) {
        return (root, query, criteriaBuilder) -> {
            List<Order> orders = new ArrayList<>();
            pageable.getSort().forEach(order -> {
                if (order.isAscending()) {
                    orders.add(criteriaBuilder.asc(root.get(order.getProperty())));
                } else {
                    orders.add(criteriaBuilder.desc(root.get(order.getProperty())));
                }
            });
            query.orderBy(orders);
            return spec.toPredicate(root, query, criteriaBuilder);
        };
    }
}
