package prm393.group8.flowermanagement.controller;

import prm393.group8.flowermanagement.entity.Product;
import prm393.group8.flowermanagement.service.CategoryService;
import prm393.group8.flowermanagement.service.ProductService;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@CrossOrigin(origins = "*")
public class HomepageController {

    private final ProductService productService;
    private final CategoryService categoryService;

    public HomepageController(ProductService productService, CategoryService categoryService) {
        this.productService = productService;
        this.categoryService = categoryService;
    }

    @GetMapping({"/", "/index", "/home"})
    public ResponseEntity<?> homepage(HttpSession session) {
        Object account = session.getAttribute("account");

        List<Product> allProducts = productService.getAllProducts();
        List<Product> highlightProducts = allProducts.stream()
                .limit(4)
                .toList();
        List<Product> featuredProducts = allProducts.stream()
                .skip(highlightProducts.size())
                .limit(5)
                .toList();
        if (featuredProducts.isEmpty()) {
            featuredProducts = highlightProducts;
        }

        Map<String, Object> response = new HashMap<>();
        response.put("account", account);
        response.put("categories", categoryService.getAllCategories());
        response.put("highlightProducts", highlightProducts);
        response.put("featuredProducts", featuredProducts);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/about-us")
    public ResponseEntity<?> aboutUs(HttpSession session) {
        Object account = session.getAttribute("account");
        Map<String, Object> response = new HashMap<>();
        response.put("account", account);
        response.put("shopName", "Tiệm Hoa Tươi Antigravity");
        response.put("description", "Chuyên cung cấp các loại hoa tươi Đà Lạt, hoa nhập khẩu Ecuador, Hà Lan chất lượng cao, thiết kế bó hoa sang trọng.");
        return ResponseEntity.ok(response);
    }
}
