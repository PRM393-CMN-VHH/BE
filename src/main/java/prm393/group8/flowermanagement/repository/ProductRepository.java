package prm393.group8.flowermanagement.repository;

import prm393.group8.flowermanagement.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<Product, Integer> {

    List<Product> findByCategory_CategoryId(int categoryId);
    List<Product> findByProductNameContainingIgnoreCase(String keyword);
    Page<Product> findByProductNameContainingIgnoreCase(String keyword, Pageable pageable);
}
