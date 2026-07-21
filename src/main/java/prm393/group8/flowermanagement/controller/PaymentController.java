package prm393.group8.flowermanagement.controller;

import prm393.group8.flowermanagement.entity.*;
import prm393.group8.flowermanagement.service.*;
import prm393.group8.flowermanagement.websocket.OrderWebSocketHandler;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.List;

@RestController
@CrossOrigin(origins = "*")
@RequestMapping("/payment")
public class PaymentController {

    private final PaymentService paymentService;
    private final OrderService orderService;
    private final OrderDetailService orderDetailService;
    private final ProductService productService;
    private final CartService cartService;
    private final NotificationService notificationService;
    private final OrderWebSocketHandler orderWebSocketHandler;

    public PaymentController(PaymentService paymentService,
                             OrderService orderService,
                             OrderDetailService orderDetailService,
                             ProductService productService,
                             CartService cartService,
                             NotificationService notificationService,
                             OrderWebSocketHandler orderWebSocketHandler) {
        this.paymentService = paymentService;
        this.orderService = orderService;
        this.orderDetailService = orderDetailService;
        this.productService = productService;
        this.cartService = cartService;
        this.notificationService = notificationService;
        this.orderWebSocketHandler = orderWebSocketHandler;
    }

    // 1. [GET/POST] /payment/create -> Trả về JSON chứa URL thanh toán
    @RequestMapping(value = "/create", method = {RequestMethod.GET, RequestMethod.POST})
    public ResponseEntity<?> createPayment(
            @RequestParam(value = "orderId", defaultValue = "1") int orderId,
            @RequestParam(value = "amount", defaultValue = "150000") long amount,
            @RequestParam(value = "bankCode", required = false) String bankCode,
            HttpServletRequest request
    ) {
        String orderInfo = "Thanh toan don hang #" + orderId;
        String paymentUrl = paymentService.createVnPayPayment(orderId, amount, orderInfo, bankCode, request);
        return ResponseEntity.ok(Map.of("paymentUrl", paymentUrl));
    }

    // 2. [GET] /payment/vnpayReturn -> Nhận callback từ VNPay và trả về kết quả JSON
    @GetMapping("/vnpayReturn")
    public ResponseEntity<?> handleVnPayReturn(@RequestParam Map<String, String> params, 
                                             HttpSession session) {
        Map<String, String> result = paymentService.handleVnPayCallback(params);
        Map<String, Object> response = new HashMap<>();

        if ("success".equals(result.get("status"))) {
            // Lấy orderId từ vnp_TxnRef (format: orderId_timestamp)
            String txnRef = result.get("orderId");
            int orderId = Integer.parseInt(txnRef.split("_")[0]);
            
            // Cập nhật Order trong database
            Optional<Order> orderOpt = orderService.getOrderById(orderId);
            if (orderOpt.isPresent()) {
                Order order = orderOpt.get();
                boolean alreadyPaid = "Paid".equalsIgnoreCase(order.getPaymentStatus());
                if (!alreadyPaid) {
                    orderService.updateOrderStatus(orderId, "PENDING", "Paid");

                    // Giảm số lượng tồn kho của từng sản phẩm sau khi thanh toán thành công
                    List<OrderDetail> orderDetails = orderDetailService.getOrderDetailsByOrderId(orderId);
                    for (OrderDetail orderDetail : orderDetails) {
                        Product product = orderDetail.getProduct();
                        if (product != null) {
                            int currentStock = product.getStock();
                            int quantityPurchased = orderDetail.getQuantity();
                            int updatedStock = Math.max(0, currentStock - quantityPurchased);
                            product.setStock(updatedStock);
                            productService.saveProduct(product);
                        }
                    }

                    // Xóa giỏ hàng của người dùng sau khi thanh toán thành công
                    User user = order.getUser();
                    if (user != null) {
                        cartService.clearCart(user);
                        notificationService.notify(
                                user,
                                "Thanh toán thành công",
                                "Giao dịch VNPay cho đơn hàng #" + orderId
                                        + " đã hoàn tất. Đơn hàng đang chờ cửa hàng xác nhận.",
                                orderId);
                        orderWebSocketHandler.pushToUser(user.getUserId(), "order_status_changed", Map.of(
                                "orderId", orderId,
                                "paymentStatus", "Paid"
                        ));
                    }
                    orderWebSocketHandler.pushToAdmins("order_updated", Map.of(
                            "orderId", orderId,
                            "paymentStatus", "Paid"
                    ));

                    // Cập nhật transaction history trong session
                    updateTransactionInHistory(session, orderId, "CONFIRMED", "Paid");
                }
            }

            response.put("status", "success");
            response.put("message", "Thanh toán thành công!");
            response.put("orderId", orderId);
            response.put("amount", result.get("amount"));
            response.put("bankCode", result.get("bankCode"));
            return ResponseEntity.ok(response);
        } else {
            response.put("status", "fail");
            response.put("message", "Thanh toán thất bại!");
            return ResponseEntity.badRequest().body(response);
        }
    }

    // 3. [GET] /payment/paymentform.html -> Trả về helper JSON
    @GetMapping("/paymentform.html")
    public ResponseEntity<?> showPaymentForm() {
        return ResponseEntity.ok(Map.of("message", "Use POST /payment/create with orderId and amount."));
    }

    /**
     * Helper method: Cập nhật transaction trong lịch sử (session)
     */
    @SuppressWarnings("unchecked")
    private void updateTransactionInHistory(HttpSession session, int orderId, String orderStatus, String paymentStatus) {
        List<TransactionHistory> history = (List<TransactionHistory>) session.getAttribute("transactionHistory");
        
        if (history != null) {
            for (TransactionHistory transaction : history) {
                if (transaction.getOrderId() == orderId) {
                    transaction.setOrderStatus(orderStatus);
                    transaction.setPaymentStatus(paymentStatus);
                    break;
                }
            }
            session.setAttribute("transactionHistory", history);
        }
    }
}
