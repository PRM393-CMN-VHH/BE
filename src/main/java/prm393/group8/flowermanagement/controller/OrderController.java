package prm393.group8.flowermanagement.controller;

import prm393.group8.flowermanagement.entity.Order;
import prm393.group8.flowermanagement.entity.OrderDetail;
import prm393.group8.flowermanagement.entity.User;
import prm393.group8.flowermanagement.service.NotificationService;
import prm393.group8.flowermanagement.service.OrderDetailService;
import prm393.group8.flowermanagement.service.OrderService;
import prm393.group8.flowermanagement.websocket.OrderWebSocketHandler;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@CrossOrigin(origins = "*")
@RequestMapping("/order")
public class OrderController {

    private final OrderService orderService;
    private final OrderDetailService orderDetailService;
    private final OrderWebSocketHandler orderWebSocketHandler;
    private final NotificationService notificationService;

    public OrderController(OrderService orderService, OrderDetailService orderDetailService,
                            OrderWebSocketHandler orderWebSocketHandler,
                            NotificationService notificationService) {
        this.orderService = orderService;
        this.orderDetailService = orderDetailService;
        this.orderWebSocketHandler = orderWebSocketHandler;
        this.notificationService = notificationService;
    }

    // 1. [GET] /order/my-orders - Danh sách đơn hàng của user, lọc theo trạng thái nếu có
    // ?status=. Hỗ trợ nhiều trạng thái cùng lúc bằng dấu phẩy, ví dụ status=DELIVERED,COMPLETED.
    @GetMapping("/my-orders")
    public ResponseEntity<?> myOrders(
            @RequestParam(value = "status", required = false) String status,
            HttpSession session) {
        User account = (User) session.getAttribute("account");
        if (account == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Not logged in"));
        }

        List<Order> orders;
        if (status == null || status.isBlank()) {
            orders = orderService.getOrdersByUserId(account.getUserId());
        } else {
            List<String> statuses = java.util.Arrays.stream(status.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .toList();
            orders = statuses.size() == 1
                    ? orderService.getOrdersByUserIdAndStatus(account.getUserId(), statuses.get(0))
                    : orderService.getOrdersByUserIdAndStatuses(account.getUserId(), statuses);
        }
        List<Map<String, Object>> responseList = new java.util.ArrayList<>();
        for (Order order : orders) {
            Map<String, Object> orderMap = new HashMap<>();
            orderMap.put("orderId", order.getOrderId());
            orderMap.put("totalPrice", order.getTotalPrice());
            orderMap.put("shippingFee", order.getShippingFee());
            orderMap.put("orderStatus", order.getOrderStatus());
            orderMap.put("paymentStatus", order.getPaymentStatus());
            orderMap.put("paymentMethod", order.getPaymentMethod());
            orderMap.put("createdAt", order.getCreatedAt());
            orderMap.put("user", order.getUser());

            List<OrderDetail> details = orderDetailService.getOrderDetailsByOrderId(order.getOrderId());
            orderMap.put("orderDetails", details);

            responseList.add(orderMap);
        }
        return ResponseEntity.ok(responseList);
    }

    // 2. [GET] /order/detail/{id} - Chi tiết đơn hàng
    @GetMapping("/detail/{id}")
    public ResponseEntity<?> orderDetail(@PathVariable("id") int id, HttpSession session) {
        User account = (User) session.getAttribute("account");
        User adminInfo = (User) session.getAttribute("adminInfo");
        if (account == null && adminInfo == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Not logged in"));
        }

        Order order = orderService.getOrderById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn hàng"));

        // Kiểm tra quyền: user chỉ xem đơn của mình, admin được phép xem tất cả các đơn hàng
        if (adminInfo == null && order.getUser().getUserId() != account.getUserId()) {
            return ResponseEntity.status(403).body(Map.of("error", "Forbidden"));
        }

        List<OrderDetail> orderDetails = orderDetailService.getOrderDetailsByOrderId(id);
        Map<String, Object> response = new HashMap<>();
        response.put("order", order);
        response.put("orderDetails", orderDetails);
        return ResponseEntity.ok(response);
    }

    // 3. [POST] /order/pay/{id} - Thanh toán lại qua VNPay
    @PostMapping("/pay/{id}")
    public ResponseEntity<?> payOrder(@PathVariable("id") int id, HttpSession session) {
        User account = (User) session.getAttribute("account");
        if (account == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Not logged in"));
        }

        Order order = orderService.getOrderById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn hàng"));

        if (order.getUser().getUserId() != account.getUserId()) {
            return ResponseEntity.status(403).body(Map.of("error", "Forbidden"));
        }

        if ("Paid".equalsIgnoreCase(order.getPaymentStatus())) {
            return ResponseEntity.badRequest().body(Map.of("error", "Đơn hàng này đã được thanh toán."));
        }

        session.setAttribute("orderId", order.getOrderId());
        long amount = Math.round(order.getTotalPrice());
        String redirectUrl = "/payment/create?orderId=" + order.getOrderId() + "&amount=" + amount;
        
        return ResponseEntity.ok(Map.of(
            "redirectUrl", redirectUrl,
            "message", "Redirect to this url to initiate VNPay payment"
        ));
    }

    // 4. [POST] /order/cancel/{id} - Hủy giao dịch
    @PostMapping("/cancel/{id}")
    public ResponseEntity<?> cancelOrder(@PathVariable("id") int id, HttpSession session) {
        User account = (User) session.getAttribute("account");
        if (account == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Not logged in"));
        }

        Order order = orderService.getOrderById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn hàng"));

        if (order.getUser().getUserId() != account.getUserId()) {
            return ResponseEntity.status(403).body(Map.of("error", "Forbidden"));
        }

        if ("Paid".equalsIgnoreCase(order.getPaymentStatus())) {
            return ResponseEntity.badRequest().body(Map.of("error", "Đơn hàng đã thanh toán không thể hủy."));
        }

        orderService.updateOrderStatus(order.getOrderId(), "CANCELLED", "Cancelled");
        orderWebSocketHandler.pushToAdmins("order_updated", Map.of("orderId", order.getOrderId(), "status", "CANCELLED"));
        return ResponseEntity.ok(Map.of("message", "Đã hủy giao dịch của đơn hàng #" + order.getOrderId(), "status", "CANCELLED"));
    }

    // 5. [POST] /order/confirm-received/{id} - Khách tự xác nhận đã nhận hàng (mở khóa đánh giá sản phẩm)
    @PostMapping("/confirm-received/{id}")
    public ResponseEntity<?> confirmReceived(@PathVariable("id") int id, HttpSession session) {
        User account = (User) session.getAttribute("account");
        if (account == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Not logged in"));
        }

        Order order = orderService.getOrderById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn hàng"));

        if (order.getUser().getUserId() != account.getUserId()) {
            return ResponseEntity.status(403).body(Map.of("error", "Forbidden"));
        }

        if (!"DELIVERED".equalsIgnoreCase(order.getOrderStatus())) {
            return ResponseEntity.badRequest().body(Map.of("error", "Chỉ có thể xác nhận khi đơn hàng đã được giao."));
        }

        Order updated = orderService.updateOrderStatus(order.getOrderId(), "COMPLETED", order.getPaymentStatus());
        orderWebSocketHandler.pushToAdmins("order_updated", Map.of("orderId", order.getOrderId(), "status", "COMPLETED"));
        notificationService.notifyAdmins(
                "Khách đã nhận hàng #" + order.getOrderId(),
                account.getFullName() + " (" + account.getEmail() + ") đã xác nhận nhận hàng cho đơn #"
                        + order.getOrderId() + ".",
                order.getOrderId());
        return ResponseEntity.ok(Map.of("message", "Đã xác nhận nhận hàng", "order", updated));
    }
}