package prm393.group8.flowermanagement.service;

import prm393.group8.flowermanagement.entity.Category;
import java.util.List;

public interface CategoryService {
    List<Category> getAllCategories();
    Category getCategoryById(int id);
}