package prm393.group8.flowermanagement.controller;

import prm393.group8.flowermanagement.entity.*;
import prm393.group8.flowermanagement.service.*;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/admin")
@CrossOrigin(origins = "*")
public class AdminController {

    private final ProductService productService;
    private final UserService userService;
    private final OrderService orderService;
    private final CategoryService categoryService;
    private final OrderDetailService orderDetailService;
    private final ProductComboItemService productComboItemService;

    public AdminController(ProductService productService,
                           UserService userService,
                           OrderService orderService,
                           CategoryService categoryService,
                           OrderDetailService orderDetailService,
                           ProductComboItemService productComboItemService) {
        this.productService = productService;
        this.userService = userService;
        this.orderService = orderService;
        this.categoryService = categoryService;
        this.orderDetailService = orderDetailService;
        this.productComboItemService = productComboItemService;
    }

    private int getComboCategoryId() {
        return categoryService.getAllCategories().stream()
                .filter(c -> "Bó Hoa / Combo".equalsIgnoreCase(c.getCategoryName()))
                .findFirst()
                .map(Category::getCategoryId)
                .orElse(8); // fallback to 8
    }

    // ================= Login page =================
    @GetMapping("")
    public ResponseEntity<?> dashboardLoginPage() {
        return ResponseEntity.ok(Map.of("message", "Please POST to /admin/login with email and password."));
    }

    @PostMapping("/login")
    public ResponseEntity<?> adminLogin(
            @RequestParam(value = "email", required = false) String emailParam,
            @RequestParam(value = "password", required = false) String passwordParam,
            @RequestBody(required = false) Map<String, String> body,
            HttpSession session
    ) {
        String email = body != null ? body.get("email") : emailParam;
        String password = body != null ? body.get("password") : passwordParam;

        if (email == null || password == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Email and password are required"));
        }

        User user = userService.getUserByEmailAndPassword(email, password);

        if (user != null) {
            if (user.getRole().getRoleId() == 1) {
                session.setAttribute("adminInfo", user);
                return ResponseEntity.ok(Map.of("status", "success", "message", "Admin login successful", "admin", user));
            }
        }
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Thông tin đăng nhập của Admin không đúng!!!"));
    }

    private User checkingAdminRole(HttpSession session) {
        User user = (User) session.getAttribute("adminInfo");
        if (user != null && user.getRole().getRoleId() == 1) {
            return user;
        }
        return null;
    }

    // ================= Dashboard =================
    @GetMapping("/dashboard")
    public ResponseEntity<?> dashboard(HttpSession session) {
        User adminInfo = checkingAdminRole(session);
        if (adminInfo == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Unauthorized access. Please login as admin."));
        }

        List<User> recentUsers = userService.getAllUser()
                .stream()
                .sorted((a, b) -> b.getUserId() - a.getUserId())
                .limit(5)
                .collect(Collectors.toList());

        Map<String, Object> stats = new HashMap<>();
        stats.put("adminInfo", adminInfo);
        stats.put("totalUsers", userService.getAllUser().size());
        stats.put("totalProducts", productService.getAllProducts().size());
        stats.put("totalOrders", orderService.getAll().size());
        stats.put("recentUsers", recentUsers);

        // Order status counts
        stats.put("pendingCount", orderService.countOrdersByStatus("PENDING"));
        stats.put("confirmedCount", orderService.countOrdersByStatus("CONFIRMED"));
        stats.put("shippedCount", orderService.countOrdersByStatus("SHIPPED"));
        stats.put("deliveredCount", orderService.countOrdersByStatus("DELIVERED"));
        stats.put("cancelledCount", orderService.countOrdersByStatus("CANCELLED"));

        return ResponseEntity.ok(stats);
    }

    // ================= Orders =================
    @GetMapping("/orders")
    public ResponseEntity<?> orders(
            HttpSession session,
            @RequestParam(value = "email", required = false) String email,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "startDate", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(value = "endDate", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(value = "pageNo", defaultValue = "1") int pageNo
    ) {
        User adminInfo = checkingAdminRole(session);
        if (adminInfo == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Unauthorized"));
        }

        if (startDate != null && endDate == null) endDate = startDate;
        if (endDate != null && startDate == null) startDate = endDate;

        int pageSize = 10;
        Page<Order> page = orderService.filterOrdersPaginated(email, status, startDate, endDate, pageNo, pageSize);

        Map<String, Object> response = new HashMap<>();
        response.put("orders", page.getContent());
        response.put("currentPage", pageNo);
        response.put("totalPage", page.getTotalPages());
        response.put("email", email);
        response.put("status", status);
        response.put("startDate", startDate);
        response.put("endDate", endDate);
        response.put("statuses", List.of("PENDING", "CONFIRMED", "SHIPPED", "DELIVERED", "CANCELLED"));

        return ResponseEntity.ok(response);
    }

    @GetMapping("/orders/detail/{id}")
    public ResponseEntity<?> orderDetail(@PathVariable("id") int id, HttpSession session) {
        User adminInfo = checkingAdminRole(session);
        if (adminInfo == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Unauthorized"));
        }

        List<OrderDetail> orderDetails = orderDetailService.getOrderDetailsByOrderId(id);
        if (orderDetails == null || orderDetails.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Không tìm thấy đơn hàng"));
        }

        return ResponseEntity.ok(Map.of("orderDetails", orderDetails));
    }

    @PostMapping("/orders/update-status")
    public ResponseEntity<?> updateOrderStatus(
            @RequestParam("orderId") int orderId,
            @RequestParam("status") String status,
            HttpSession session
    ) {
        User adminInfo = checkingAdminRole(session);
        if (adminInfo == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Unauthorized"));
        }

        orderService.updateOrderStatusById(orderId, status);
        
        // Trả lại số lượng tồn kho của từng sản phẩm sau khi hủy đơn hàng
        if ("CANCELLED".equalsIgnoreCase(status)) {
            List<OrderDetail> orderDetails = orderDetailService.getOrderDetailsByOrderId(orderId);
            int comboCategoryId = getComboCategoryId();
            for (OrderDetail orderDetail : orderDetails) {
                Product product = orderDetail.getProduct();
                if (product != null) {
                    if (product.getCategory().getCategoryId() == comboCategoryId) {
                        List<ProductComboItem> productComboItems = productComboItemService.getItemsByComboId(product.getProductId());
                        for (ProductComboItem item : productComboItems) {
                            int productComboItemStock = item.getComponent().getStock();
                            int remainingStock = productComboItemStock + (orderDetail.getQuantity() * item.getQuantity());
                            item.getComponent().setStock(remainingStock);
                        }
                    }
                    int currentStock = product.getStock();
                    int quantityPurchased = orderDetail.getQuantity();
                    int updatedStock = Math.max(0, currentStock + quantityPurchased);
                    product.setStock(updatedStock);
                    productService.saveProduct(product);
                }
            }
        }
        return ResponseEntity.ok(Map.of("message", "Cập nhật trạng thái thành công!", "status", status));
    }

    @DeleteMapping("/orders/delete/{id}")
    public ResponseEntity<?> deleteOrder(@PathVariable("id") int id, HttpSession session) {
        User adminInfo = checkingAdminRole(session);
        if (adminInfo == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Unauthorized"));
        }

        orderService.getOrderById(id).ifPresent(order -> orderService.getAll().remove(order));
        return ResponseEntity.ok(Map.of("message", "Order deleted successfully"));
    }

    // Legacy GET delete endpoint
    @GetMapping("/orders/delete/{id}")
    public ResponseEntity<?> deleteOrderLegacy(@PathVariable("id") int id, HttpSession session) {
        return deleteOrder(id, session);
    }

    // ================= Products =================
    @GetMapping("/products")
    public ResponseEntity<?> products(
            HttpSession session,
            @RequestParam(value = "pageNo", defaultValue = "1") int pageNo,
            @RequestParam(value = "keyword", required = false) String searchQuery
    ) {
        User adminInfo = checkingAdminRole(session);
        if (adminInfo == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Unauthorized"));
        }

        int pageSize = 10;
        Page<Product> page;

        if (searchQuery != null && !searchQuery.trim().isEmpty()) {
            page = productService.searchPaginatedProducts(searchQuery, pageNo, pageSize);
        } else {
            page = productService.getPaginatedProducts(pageNo, pageSize);
        }

        Map<String, Object> response = new HashMap<>();
        response.put("products", page.getContent());
        response.put("currentPage", pageNo);
        response.put("totalPage", page.getTotalPages());
        response.put("categories", categoryService.getAllCategories());

        return ResponseEntity.ok(response);
    }

    @PostMapping("/products/add")
    public ResponseEntity<?> addProduct(
            @Valid @RequestBody Product product,
            BindingResult bindingResult,
            HttpSession session
    ) {
        User adminInfo = checkingAdminRole(session);
        if (adminInfo == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Unauthorized"));
        }

        if (bindingResult.hasErrors()) {
            return ResponseEntity.badRequest().body(bindingResult.getAllErrors());
        }

        Product saved = productService.saveProduct(product);
        return ResponseEntity.ok(saved);
    }

    @GetMapping("/products/combo")
    public ResponseEntity<?> addProductComboPage(HttpSession session) {
        User adminInfo = checkingAdminRole(session);
        if (adminInfo == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Unauthorized"));
        }

        int comboCategoryId = getComboCategoryId();
        List<Product> comboProducts = productService.getProductsByCategoryId(comboCategoryId);
        return ResponseEntity.ok(comboProducts);
    }

    @GetMapping("/products/combo/add-item/{id}")
    public ResponseEntity<?> addProductComboItemPage(
            HttpSession session,
            @PathVariable("id") int id
    ) {
        User adminInfo = checkingAdminRole(session);
        if (adminInfo == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Unauthorized"));
        }

        Product productParent = productService.getProductById(id).orElse(null);
        if (productParent == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Không tìm thấy sản phẩm combo"));
        }

        int comboCategoryId = getComboCategoryId();
        List<ProductComboItem> comboItems = productComboItemService.getItemsByComboId(id);
        List<Product> productList = productService.getAllProductsByCategoryIdNot(comboCategoryId);

        Map<String, Object> response = new HashMap<>();
        response.put("productParent", productParent);
        response.put("comboItems", comboItems);
        response.put("productList", productList);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/products/combo/add-item")
    public ResponseEntity<?> addProductToCombo(
            @RequestParam("comboId") int comboId,
            @RequestParam("productId") int productId,
            @RequestParam("quantity") int quantity
    ) {
        Product item = productService.getProductById(productId).orElse(null);
        Product productParent = productService.getProductById(comboId).orElse(null);
        if (item == null || productParent == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Lỗi sản phẩm"));
        }

        ProductComboItem existing = productComboItemService.getByComboAndComponent(productParent, item);
        if (existing != null) {
            existing.setQuantity(existing.getQuantity() + quantity);
            productComboItemService.save(existing);
        } else {
            ProductComboItem pci = new ProductComboItem(productParent, item, quantity);
            productComboItemService.save(pci);
        }

        List<ProductComboItem> comboItems = productComboItemService.getItemsByComboId(comboId);
        return ResponseEntity.ok(comboItems);
    }

    @DeleteMapping("/products/combo/remove-item/{id}")
    public ResponseEntity<?> removeProductFromCombo(@PathVariable("id") int id) {
        ProductComboItem pci = productComboItemService.getById(id);
        if (pci != null) {
            int comboId = pci.getCombo().getProductId();
            productComboItemService.delete(pci);
            List<ProductComboItem> comboItems = productComboItemService.getItemsByComboId(comboId);
            return ResponseEntity.ok(comboItems);
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Combo item not found"));
    }

    // Legacy GET mapping
    @GetMapping("/products/combo/remove-item/{id}")
    public ResponseEntity<?> removeProductFromComboLegacy(@PathVariable("id") int id) {
        return removeProductFromCombo(id);
    }

    @DeleteMapping("/products/delete/{id}")
    public ResponseEntity<?> deleteProduct(@PathVariable("id") int id, HttpSession session) {
        User adminInfo = checkingAdminRole(session);
        if (adminInfo == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Unauthorized"));
        }

        productService.deleteProduct(id);
        return ResponseEntity.ok(Map.of("message", "Product deleted successfully"));
    }

    // Legacy GET delete
    @GetMapping("/products/delete/{id}")
    public ResponseEntity<?> deleteProductLegacy(@PathVariable("id") int id, HttpSession session) {
        return deleteProduct(id, session);
    }

    @GetMapping("/products/edit/{id}")
    public ResponseEntity<?> editProductPage(
            @PathVariable("id") int id,
            HttpSession session
    ) {
        User adminInfo = checkingAdminRole(session);
        if (adminInfo == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Unauthorized"));
        }
        Product product = productService.getProductById(id).orElse(null);
        if (product == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Không tìm thấy sản phẩm"));
        }
        return ResponseEntity.ok(product);
    }

    @PostMapping("/products/edit")
    public ResponseEntity<?> editProduct(
            @Valid @RequestBody Product product,
            BindingResult bindingResult,
            HttpSession session
    ) {
        User adminInfo = checkingAdminRole(session);
        if (adminInfo == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Unauthorized"));
        }
        if (bindingResult.hasErrors()) {
            return ResponseEntity.badRequest().body(bindingResult.getAllErrors());
        }
        Product saved = productService.saveProduct(product);
        return ResponseEntity.ok(saved);
    }

    // ================= Users =================
    @GetMapping("/users")
    public ResponseEntity<?> users(
            HttpSession session,
            @RequestParam(value = "pageNo", defaultValue = "1") int pageNo,
            @RequestParam(value = "search", required = false) String searchQuery
    ) {
        User adminInfo = checkingAdminRole(session);
        if (adminInfo == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Unauthorized"));
        }

        int pageSize = 10;
        Page<User> page;

        if (searchQuery != null && !searchQuery.trim().isEmpty()) {
            page = userService.searchPaginatedUsersByFullName(searchQuery, pageNo, pageSize);
        } else {
            page = userService.getPaginatedUsers(pageNo, pageSize);
        }

        Map<String, Object> response = new HashMap<>();
        response.put("users", page.getContent());
        response.put("currentPage", pageNo);
        response.put("totalPage", page.getTotalPages());

        return ResponseEntity.ok(response);
    }

    @PostMapping("/users/add")
    public ResponseEntity<?> addUser(@RequestBody User newUser, HttpSession session) {
        User adminInfo = checkingAdminRole(session);
        if (adminInfo == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Unauthorized"));
        }
        userService.addUser(newUser);
        return ResponseEntity.ok(newUser);
    }

    @PostMapping("/users/update")
    public ResponseEntity<?> updateUser(@RequestParam("userId") int userId,
                                         @RequestParam("fullName") String fullName,
                                         @RequestParam("email") String email,
                                         @RequestParam("phoneNumber") String phoneNumber,
                                         HttpSession session) {
        User adminInfo = checkingAdminRole(session);
        if (adminInfo == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Unauthorized"));
        }
        User existing = userService.findById(userId);
        if (existing != null) {
            existing.setFullName(fullName);
            existing.setEmail(email);
            existing.setPhoneNumber(phoneNumber);
            userService.updateProfile(existing);
            return ResponseEntity.ok(existing);
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "User not found"));
    }

    @PostMapping("/users/deactivate/{id}")
    public ResponseEntity<?> deactivateUser(@PathVariable("id") int id, HttpSession session) {
        User adminInfo = checkingAdminRole(session);
        if (adminInfo == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Unauthorized"));
        }
        User existing = userService.findById(id);
        if (existing != null) {
            existing.setStatus(false);
            userService.saveUser(existing);
            return ResponseEntity.ok(existing);
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "User not found"));
    }

    // Legacy GET mapping
    @GetMapping("/users/deactivate/{id}")
    public ResponseEntity<?> deactivateUserLegacy(@PathVariable("id") int id, HttpSession session) {
        return deactivateUser(id, session);
    }

    @PostMapping("/users/activate/{id}")
    public ResponseEntity<?> activeUser(@PathVariable("id") int id, HttpSession session) {
        User adminInfo = checkingAdminRole(session);
        if (adminInfo == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Unauthorized"));
        }
        User existing = userService.findById(id);
        if (existing != null) {
            existing.setStatus(true);
            userService.saveUser(existing);
            return ResponseEntity.ok(existing);
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "User not found"));
    }

    // Legacy GET mapping
    @GetMapping("/users/activate/{id}")
    public ResponseEntity<?> activeUserLegacy(@PathVariable("id") int id, HttpSession session) {
        return activeUser(id, session);
    }

    @GetMapping("/logout")
    public ResponseEntity<?> adminLogout(HttpSession session) {
        session.invalidate();
        return ResponseEntity.ok(Map.of("message", "Admin logged out successfully"));
    }
}
