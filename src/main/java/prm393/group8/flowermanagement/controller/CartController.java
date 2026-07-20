package prm393.group8.flowermanagement.controller;

import prm393.group8.flowermanagement.entity.CartItem;
import prm393.group8.flowermanagement.entity.Order;
import prm393.group8.flowermanagement.entity.OrderDetail;
import prm393.group8.flowermanagement.entity.Product;
import prm393.group8.flowermanagement.entity.User;
import prm393.group8.flowermanagement.service.CartService;
import prm393.group8.flowermanagement.service.NotificationService;
import prm393.group8.flowermanagement.service.OrderDetailService;
import prm393.group8.flowermanagement.service.OrderService;
import prm393.group8.flowermanagement.service.ProductService;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@CrossOrigin(origins = "*")
@RequestMapping("/cart")
public class CartController {

    // Flat shipping fee, waived for orders at/above the free-shipping threshold.
    private static final double SHIPPING_FEE = 30_000;
    private static final double FREE_SHIPPING_THRESHOLD = 500_000;

    private static double shippingFeeFor(double subtotal) {
        return subtotal >= FREE_SHIPPING_THRESHOLD ? 0 : SHIPPING_FEE;
    }

    private final ProductService productService;
    private final OrderService orderService;
    private final OrderDetailService orderDetailService;
    private final CartService cartService;
    private final NotificationService notificationService;

    public CartController(ProductService productService,
                          OrderService orderService,
                          OrderDetailService orderDetailService,
                          CartService cartService,
                          NotificationService notificationService) {
        this.productService = productService;
        this.orderService = orderService;
        this.orderDetailService = orderDetailService;
        this.cartService = cartService;
        this.notificationService = notificationService;
    }

    // 1. [GET] /cart - Xem giỏ hàng
    @GetMapping
    public ResponseEntity<?> viewCart(HttpSession session) {
        User account = (User) session.getAttribute("account");
        if (account == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Not logged in", "redirect", "/login"));
        }

        List<CartItem> cart = cartService.getCartByUser(account);

        double total = cart.stream()
                .mapToDouble(CartItem::getSubtotal)
                .sum();

        Map<String, Object> response = new HashMap<>();
        response.put("cart", cart);
        response.put("total", total);
        response.put("account", account);

        // Hiển thị thông báo lỗi/thành công (nếu có)
        String errorMessage = (String) session.getAttribute("errorMessage");
        String successMessage = (String) session.getAttribute("successMessage");

        if (errorMessage != null) {
            response.put("errorMessage", errorMessage);
            session.removeAttribute("errorMessage");
        }

        if (successMessage != null) {
            response.put("successMessage", successMessage);
            session.removeAttribute("successMessage");
        }

        return ResponseEntity.ok(response);
    }

    // 2. [POST] /cart/add - Thêm sản phẩm vào giỏ
    @PostMapping("/add")
    public ResponseEntity<?> addToCart(@RequestParam(value = "productId", required = false) Integer productIdParam,
                                       @RequestParam(value = "quantity", defaultValue = "1") int quantityParam,
                                       @RequestBody(required = false) Map<String, Object> body,
                                       HttpSession session) {
        User account = (User) session.getAttribute("account");
        if (account == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Not logged in", "redirect", "/login"));
        }

        Integer productId = productIdParam;
        int quantity = quantityParam;

        if (body != null) {
            if (body.containsKey("productId")) {
                productId = Integer.parseInt(body.get("productId").toString());
            }
            if (body.containsKey("quantity")) {
                quantity = Integer.parseInt(body.get("quantity").toString());
            }
        }

        if (productId == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "productId is required"));
        }

        Product product = productService.getProductById(productId)
                .orElse(null);
        if (product == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Không tìm thấy sản phẩm"));
        }

        // Kiểm tra số lượng không được <= 0
        if (quantity <= 0) {
            return ResponseEntity.badRequest().body(Map.of("error", "Số lượng phải lớn hơn 0!"));
        }

        // Kiểm tra xem sản phẩm đã có trong giỏ chưa
        List<CartItem> cart = cartService.getCartByUser(account);
        CartItem existingItem = cart.stream()
                .filter(item -> item.getProduct().getProductId() == product.getProductId())
                .findFirst()
                .orElse(null);

        int newQuantity = quantity;
        if (existingItem != null) {
            newQuantity += existingItem.getQuantity();
        }

        // Kiểm tra không vượt quá tồn kho
        if (newQuantity > product.getStock()) {
            return ResponseEntity.badRequest().body(Map.of("error",
                    "Không thể thêm! Sản phẩm '" + product.getProductName() +
                            "' chỉ còn " + product.getStock() + " sản phẩm trong kho."));
        }

        cartService.addToCart(account, product, quantity);

        List<CartItem> updatedCart = cartService.getCartByUser(account);
        return ResponseEntity.ok(Map.of("message", "Đã thêm sản phẩm vào giỏ hàng!", "cart", updatedCart));
    }

    // 3. [POST] /cart/update - Cập nhật số lượng
    @PostMapping("/update")
    public ResponseEntity<?> updateCart(@RequestParam(value = "productId", required = false) Integer productIdParam,
                                        @RequestParam(value = "quantity", required = false) Integer quantityParam,
                                        @RequestBody(required = false) Map<String, Object> body,
                                        HttpSession session) {
        User account = (User) session.getAttribute("account");
        if (account == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Not logged in", "redirect", "/login"));
        }

        Integer productId = productIdParam;
        Integer quantity = quantityParam;

        if (body != null) {
            if (body.containsKey("productId")) {
                productId = Integer.parseInt(body.get("productId").toString());
            }
            if (body.containsKey("quantity")) {
                quantity = Integer.parseInt(body.get("quantity").toString());
            }
        }

        if (productId == null || quantity == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "productId and quantity are required"));
        }

        List<CartItem> cart = cartService.getCartByUser(account);
        final int targetProductId = productId;
        CartItem cartItem = cart.stream()
                .filter(item -> item.getProduct().getProductId() == targetProductId)
                .findFirst()
                .orElse(null);

        if (cartItem != null) {
            Product product = cartItem.getProduct();

            // Kiểm tra số lượng phải > 0
            if (quantity <= 0) {
                return ResponseEntity.badRequest().body(Map.of("error", "Số lượng phải lớn hơn 0!"));
            }

            // Kiểm tra không vượt quá tồn kho
            if (quantity > product.getStock()) {
                return ResponseEntity.badRequest().body(Map.of("error",
                        "Sản phẩm '" + product.getProductName() +
                                "' chỉ còn " + product.getStock() + " sản phẩm trong kho. Không thể cập nhật!"));
            }

            cartService.updateCart(account, product, quantity);
        } else {
            return ResponseEntity.badRequest().body(Map.of("error", "Sản phẩm không có trong giỏ hàng."));
        }

        List<CartItem> updatedCart = cartService.getCartByUser(account);
        return ResponseEntity.ok(Map.of("message", "Đã cập nhật số lượng!", "cart", updatedCart));
    }

    // 4. [POST] /cart/remove - Xóa sản phẩm khỏi giỏ
    @PostMapping("/remove")
    public ResponseEntity<?> removeFromCart(@RequestParam(value = "productId", required = false) Integer productIdParam,
                                           @RequestBody(required = false) Map<String, Object> body,
                                           HttpSession session) {
        User account = (User) session.getAttribute("account");
        if (account == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Not logged in", "redirect", "/login"));
        }

        Integer productId = productIdParam;
        if (body != null && body.containsKey("productId")) {
            productId = Integer.parseInt(body.get("productId").toString());
        }

        if (productId == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "productId is required"));
        }

        Product product = productService.getProductById(productId).orElse(null);
        if (product == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Không tìm thấy sản phẩm"));
        }

        cartService.removeFromCart(account, product);

        List<CartItem> updatedCart = cartService.getCartByUser(account);
        return ResponseEntity.ok(Map.of("message", "Đã xóa sản phẩm khỏi giỏ hàng!", "cart", updatedCart));
    }

    // 5. [GET] /cart/checkout - Trang checkout
    @GetMapping("/checkout")
    public ResponseEntity<?> checkoutPage(HttpSession session) {
        User account = (User) session.getAttribute("account");
        if (account == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Not logged in", "redirect", "/login"));
        }

        List<CartItem> cart = cartService.getCartByUser(account);
        if (cart.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Giỏ hàng trống", "redirect", "/cart"));
        }

        double subtotal = cart.stream()
                .mapToDouble(CartItem::getSubtotal)
                .sum();
        double shippingFee = shippingFeeFor(subtotal);

        Map<String, Object> response = new HashMap<>();
        response.put("cart", cart);
        response.put("subtotal", subtotal);
        response.put("shippingFee", shippingFee);
        response.put("total", subtotal + shippingFee);
        response.put("account", account);
        return ResponseEntity.ok(response);
    }

    // 6. [POST] /cart/place-order - Đặt hàng (VNPay hoặc COD)
    @PostMapping("/place-order")
    public ResponseEntity<?> placeOrder(@RequestParam(value = "paymentMethod", required = false) String paymentMethodParam,
                                       @RequestBody(required = false) Map<String, Object> body,
                                       HttpSession session) {
        User account = (User) session.getAttribute("account");
        if (account == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Not logged in", "redirect", "/login"));
        }

        String paymentMethod = paymentMethodParam;
        if (body != null && body.containsKey("paymentMethod")) {
            paymentMethod = body.get("paymentMethod").toString();
        }

        if (paymentMethod == null || paymentMethod.isEmpty()) {
            paymentMethod = "COD"; // default
        }

        List<CartItem> cart = cartService.getCartByUser(account);
        if (cart.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Giỏ hàng trống", "redirect", "/cart"));
        }

        // Tính tổng tiền (tạm tính + phí giao hàng)
        double subtotal = cart.stream()
                .mapToDouble(CartItem::getSubtotal)
                .sum();
        double shippingFee = shippingFeeFor(subtotal);
        double totalPrice = subtotal + shippingFee;

        // Tạo Order
        Order order = new Order();
        order.setUser(account);
        order.setTotalPrice(totalPrice);
        order.setShippingFee(shippingFee);
        order.setPaymentMethod(paymentMethod);
        order.setOrderStatus("Pending");
        order.setPaymentStatus("Unpaid");

        Order savedOrder = orderService.createOrder(order);

        // Tạo OrderDetails (giá theo khuyến mãi nếu có)
        for (CartItem item : cart) {
            Product product = item.getProduct();
            OrderDetail orderDetail = new OrderDetail();
            orderDetail.setOrder(savedOrder);
            orderDetail.setProduct(product);
            orderDetail.setQuantity(item.getQuantity());
            orderDetail.setPrice(product.getPromoPrice() != null ? product.getPromoPrice() : product.getPrice());
            orderDetailService.saveOrderDetail(orderDetail);
        }

        notificationService.notify(
                account,
                "Đặt hàng thành công",
                "Đơn hàng #" + savedOrder.getOrderId() + " đã được tạo với tổng tiền "
                        + String.format("%,.0f", totalPrice) + " đ. Chúng tôi sẽ sớm xác nhận đơn của bạn.");

        // Xóa giỏ hàng khỏi DB (Chỉ xóa ngay nếu không phải thanh toán VNPay)
        if (!"VNPay".equalsIgnoreCase(paymentMethod)) {
            cartService.clearCart(account);
        }

        Map<String, Object> response = new HashMap<>();
        response.put("order", savedOrder);
        response.put("message", "Đặt hàng thành công!");

        // Nếu chọn VNPay → Redirect đến trang thanh toán VNPay
        if ("VNPay".equalsIgnoreCase(paymentMethod)) {
            session.setAttribute("orderId", savedOrder.getOrderId());
            String redirectUrl = "/payment/create?orderId=" + savedOrder.getOrderId() + "&amount=" + (long) totalPrice;
            response.put("paymentMethod", "VNPay");
            response.put("redirectUrl", redirectUrl);
        } else {
            response.put("paymentMethod", "COD");
            response.put("redirectUrl", "/order/detail/" + savedOrder.getOrderId());
        }

        return ResponseEntity.ok(response);
    }
}
