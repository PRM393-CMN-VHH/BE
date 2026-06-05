package prm393.group8.flowermanagement.repository;

import prm393.group8.flowermanagement.entity.Product;
import prm393.group8.flowermanagement.entity.ProductComboItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductComboItemRepository extends JpaRepository<ProductComboItem, Integer> {

    List<ProductComboItem> getAllByCombo_ProductId(int comboProductId);

    ProductComboItem findProductComboItemByComboAndComponent(Product combo, Product component);
}
