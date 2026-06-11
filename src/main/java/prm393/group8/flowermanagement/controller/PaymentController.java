package prm393.group8.flowermanagement.controller;

import prm393.group8.flowermanagement.entity.*;
import prm393.group8.flowermanagement.service.*;
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
public class PaymentController {

    private final PaymentService paymentService;
    private final OrderService orderService;
    private final OrderDetailService orderDetailService;
    private final ProductService productService;
    private final ProductComboItemService productComboItemService;

    public PaymentController(PaymentService paymentService,
                             OrderService orderService,
                             OrderDetailService orderDetailService,
                             ProductService productService,
                             ProductComboItemService productComboItemService) {
        this.paymentService = paymentService;
        this.orderService = orderService;
        this.orderDetailService = orderDetailService;
        this.productService = productService;
        this.productComboItemService = productComboItemService;
    }

    // 1. [POST] /payment/create -> Trả về JSON chứa URL thanh toán
    @PostMapping("/payment/create")
    public ResponseEntity<?> createPayment(
            @RequestParam(value = "orderId", defaultValue = "1") int orderId,
            @RequestParam(value = "amount", defaultValue = "150000") long amount,
            @RequestParam(value = "bankCode", required = false) String bankCode,
            HttpServletRequest request
    ) {
        String orderInfo = "Thanh toan don hang " + orderId;
        String paymentUrl = paymentService.createVnPayPayment(orderId, amount, orderInfo, bankCode, request);
        return ResponseEntity.ok(Map.of("paymentUrl", paymentUrl));
    }

    // Tạo thanh toán từ Cart (GET) -> Trả về JSON chứa URL thanh toán
    @GetMapping("/payment/create")
    public ResponseEntity<?> createPaymentFromCart(
            @RequestParam("orderId") int orderId,
            @RequestParam("amount") long amount,
            @RequestParam(value = "bankCode", required = false) String bankCode,
            HttpServletRequest request
    ) {
        String orderInfo = "Thanh toan don hang #" + orderId;
        String paymentUrl = paymentService.createVnPayPayment(orderId, amount, orderInfo, bankCode, request);
        return ResponseEntity.ok(Map.of("paymentUrl", paymentUrl));
    }

    // 2. [GET] /payment/vnpayReturn -> Nhận callback từ VNPay và trả về kết quả JSON
    @GetMapping("/payment/vnpayReturn")
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
                orderService.updateOrderStatus(orderId, "PENDING", "Paid");

                // Giảm số lượng tồn kho của từng sản phẩm sau khi thanh toán thành công
                List<OrderDetail> orderDetails = orderDetailService.getOrderDetailsByOrderId(orderId);
                for (OrderDetail orderDetail : orderDetails) {
                    Product product = orderDetail.getProduct();
                    if (product != null) {
                        // Kiểm tra nếu là danh mục Combo/Bó hoa bằng tên thay vì ID cứng
                        if ("Bó Hoa / Combo".equalsIgnoreCase(product.getCategory().getCategoryName())) {
                            List<ProductComboItem> productComboItems = productComboItemService.getItemsByComboId(product.getProductId());
                            for (ProductComboItem item : productComboItems) {
                                int productComboItemStock = item.getComponent().getStock();
                                int remainingStock = productComboItemStock - (orderDetail.getQuantity() * item.getQuantity());
                                item.getComponent().setStock(Math.max(0, remainingStock));
                            }
                        }
                        int currentStock = product.getStock();
                        int quantityPurchased = orderDetail.getQuantity();
                        int updatedStock = Math.max(0, currentStock - quantityPurchased);
                        product.setStock(updatedStock);
                        productService.saveProduct(product);
                    }
                }

                // Cập nhật transaction history trong session
                updateTransactionInHistory(session, orderId, "CONFIRMED", "Paid");
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
    @GetMapping("/payment/paymentform.html")
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