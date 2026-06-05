package prm393.group8.flowermanagement.controller;

import prm393.group8.flowermanagement.entity.Category;
import prm393.group8.flowermanagement.entity.Product;
import prm393.group8.flowermanagement.service.CategoryService;
import prm393.group8.flowermanagement.service.ProductService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.text.Normalizer;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@RestController
@CrossOrigin(origins = "*")
public class ProductController {

    private final ProductService productService;
    private final CategoryService categoryService;

    public ProductController(ProductService productService, CategoryService categoryService) {
        this.productService = productService;
        this.categoryService = categoryService;
    }

    // 1. [GET] /admin/product (Admin) -> JSON list
    @GetMapping("/admin/product")
    public ResponseEntity<List<Product>> listProducts() {
        return ResponseEntity.ok(productService.getAllProducts());
    }

    // 2. [GET] /admin/product/categories (Helper) -> JSON list
    @GetMapping("/admin/product/categories")
    public ResponseEntity<List<Category>> getAdminCategories() {
        return ResponseEntity.ok(categoryService.getAllCategories());
    }

    // 3. [POST] /admin/product/save (Admin) -> JSON saved product
    @PostMapping("/admin/product/save")
    public ResponseEntity<?> saveProduct(@Valid @RequestBody Product product) {
        Product saved = productService.saveProduct(product);
        return ResponseEntity.ok(saved);
    }

    // 4. [GET] /admin/product/edit/{id} (Admin) -> JSON details
    @GetMapping("/admin/product/edit/{id}")
    public ResponseEntity<?> showEditProductForm(@PathVariable("id") int id) {
        Optional<Product> product = productService.getProductById(id);
        if (product.isPresent()) {
            Map<String, Object> response = new HashMap<>();
            response.put("product", product.get());
            response.put("categories", categoryService.getAllCategories());
            return ResponseEntity.ok(response);
        }
        return ResponseEntity.notFound().build();
    }

    // 5. [POST] /admin/product/edit/{id} (Admin) -> JSON updated product
    @PostMapping("/admin/product/edit/{id}")
    public ResponseEntity<?> updateProduct(@PathVariable("id") int id, @Valid @RequestBody Product product) {
        Optional<Product> existing = productService.getProductById(id);
        if (existing.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        product.setProductId(id);
        Product saved = productService.saveProduct(product);
        return ResponseEntity.ok(saved);
    }

    // 6. [DELETE] /admin/product/delete/{id} (Admin) -> JSON confirmation
    @DeleteMapping("/admin/product/delete/{id}")
    public ResponseEntity<?> deleteProductAdmin(@PathVariable("id") int id) {
        productService.deleteProduct(id);
        return ResponseEntity.ok(Map.of("message", "Product deleted successfully"));
    }

    // Legacy GET mapping for delete if needed
    @GetMapping("/admin/product/delete/{id}")
    public ResponseEntity<?> deleteProductAdminLegacy(@PathVariable("id") int id) {
        productService.deleteProduct(id);
        return ResponseEntity.ok(Map.of("message", "Product deleted successfully"));
    }

    // 7. [GET] /product/category/{categoryId} (User) -> JSON details
    @GetMapping("/product/category/{categoryId}")
    public ResponseEntity<?> showProductsByCategory(@PathVariable("categoryId") int categoryId) {
        Category category = categoryService.getCategoryById(categoryId);
        if (category == null) {
            return ResponseEntity.notFound().build();
        }

        List<Product> products = productService.getProductsByCategoryId(categoryId);
        Map<String, Object> response = new HashMap<>();
        response.put("category", category);
        response.put("products", products);
        response.put("totalProducts", products.size());
        response.put("categories", categoryService.getAllCategories());
        return ResponseEntity.ok(response);
    }

    // 8. [GET] /products/{id} (User) -> JSON details
    @GetMapping("/products/{id}")
    public ResponseEntity<?> showProductDetail(@PathVariable("id") int id) {
        Optional<Product> productOpt = productService.getProductById(id);
        if (productOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Product product = productOpt.get();
        List<Product> relatedProducts = productService.getProductsByCategoryId(product.getCategory().getCategoryId())
                .stream()
                .filter(p -> p.getProductId() != product.getProductId())
                .limit(4)
                .toList();

        Map<String, Object> response = new HashMap<>();
        response.put("product", product);
        response.put("relatedProducts", relatedProducts);
        return ResponseEntity.ok(response);
    }

    // 8b. [GET] /product/detail/{id} (User - legacy url) -> JSON details
    @GetMapping("/product/detail/{id}")
    public ResponseEntity<?> showProductDetailLegacy(@PathVariable("id") int id) {
        return showProductDetail(id);
    }

    // 9. [GET] /product/all-product (User) -> JSON list
    @GetMapping("/product/all-product")
    public ResponseEntity<List<Product>> showAllProducts() {
        return ResponseEntity.ok(productService.getAllProducts());
    }

    // 10. [POST] /product/search (User) -> JSON list
    @PostMapping("/product/search")
    public ResponseEntity<?> searchProducts(@RequestParam("keyword") String keyword) {
        String trimmedKeyword = keyword != null ? keyword.trim() : "";
        if (trimmedKeyword.isEmpty()) {
            return ResponseEntity.ok(Collections.emptyList());
        }

        List<Product> products = productService.searchProducts(keyword);
        return ResponseEntity.ok(products);
    }

    // 11. [GET] /api/products/suggest (User - ajax) -> JSON list
    @GetMapping("/api/products/suggest")
    public ResponseEntity<List<Map<String, Object>>> suggestProducts(@RequestParam("keyword") String keyword) {
        if (keyword == null) {
            return ResponseEntity.ok(Collections.emptyList());
        }

        String trimmedKeyword = keyword.trim();
        if (trimmedKeyword.isEmpty()) {
            return ResponseEntity.ok(Collections.emptyList());
        }

        String normalizedKeyword = normalize(trimmedKeyword);
        if (normalizedKeyword.isEmpty()) {
            return ResponseEntity.ok(Collections.emptyList());
        }

        List<Map<String, Object>> suggestions = productService.getAllProducts()
                .stream()
                .filter(product -> {
                    String normalizedName = normalize(product.getProductName());
                    return normalizedName.contains(normalizedKeyword);
                })
                .limit(8)
                .map(product -> {
                    Map<String, Object> suggestion = new HashMap<>();
                    suggestion.put("id", product.getProductId());
                    suggestion.put("name", product.getProductName());
                    suggestion.put("imageUrl", product.getImageUrl());
                    return suggestion;
                })
                .collect(Collectors.toList());

        return ResponseEntity.ok(suggestions);
    }

    private String normalize(String input) {
        String normalized = Normalizer.normalize(input.toLowerCase(Locale.ROOT).trim(), Normalizer.Form.NFD);
        return Pattern.compile("\\p{M}+").matcher(normalized).replaceAll("");
    }
}