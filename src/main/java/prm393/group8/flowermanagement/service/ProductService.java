package prm393.group8.flowermanagement.service;

import prm393.group8.flowermanagement.entity.Product;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.Optional;

public interface ProductService {
    List<Product> getAllProducts();
    Page<Product> getPaginatedProducts(int pageNo, int pageSize);
    Optional<Product> getProductById(int id);
    Product saveProduct(Product product);
    void deleteProduct(int id);
    List<Product> getProductsByCategoryId(int categoryId);
    List<Product> searchProducts(String keyword);
    Page<Product> searchPaginatedProducts(String keyword, int pageNo, int pageSize);
    List<Product> getAllProductsByCategoryIdNot(int categoryId);
}
