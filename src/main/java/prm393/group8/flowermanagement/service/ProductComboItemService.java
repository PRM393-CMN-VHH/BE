package prm393.group8.flowermanagement.service;

import prm393.group8.flowermanagement.entity.Product;
import prm393.group8.flowermanagement.entity.ProductComboItem;

import java.util.List;

public interface ProductComboItemService {
    ProductComboItem getById(int id);
    public List<ProductComboItem> getItemsByComboId(int comboId);
    void save(ProductComboItem productComboItem);
    ProductComboItem getByComboAndComponent(Product combo, Product component);
    void delete(ProductComboItem productComboItem);
}
