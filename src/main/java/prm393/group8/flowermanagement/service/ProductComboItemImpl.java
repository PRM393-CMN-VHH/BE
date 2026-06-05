package prm393.group8.flowermanagement.service;

import prm393.group8.flowermanagement.entity.Product;
import prm393.group8.flowermanagement.entity.ProductComboItem;
import prm393.group8.flowermanagement.repository.ProductComboItemRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProductComboItemImpl implements ProductComboItemService {

    private final ProductComboItemRepository productComboItemRepository;

    public ProductComboItemImpl(ProductComboItemRepository productComboItemRepository) {
        this.productComboItemRepository = productComboItemRepository;
    }

    @Override
    public ProductComboItem getById(int id) {
        return productComboItemRepository.findById(id).orElse(null);
    }

    @Override
    public List<ProductComboItem> getItemsByComboId(int comboId) {
        return productComboItemRepository.getAllByCombo_ProductId(comboId);
    }

    @Override
    public void save(ProductComboItem productComboItem) {
        productComboItemRepository.save(productComboItem);
    }

    @Override
    public ProductComboItem getByComboAndComponent(Product combo, Product component) {
        return productComboItemRepository.findProductComboItemByComboAndComponent(combo, component);
    }

    @Override
    public void delete(ProductComboItem productComboItem) {
        productComboItemRepository.delete(productComboItem);
    }
}
