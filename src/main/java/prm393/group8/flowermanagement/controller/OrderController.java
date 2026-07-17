package prm393.group8.flowermanagement.controller;

import prm393.group8.flowermanagement.entity.Order;
import prm393.group8.flowermanagement.entity.OrderDetail;
import prm393.group8.flowermanagement.entity.User;
import prm393.group8.flowermanagement.service.OrderDetailService;
import prm393.group8.flowermanagement.service.OrderService;
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

    public OrderController(OrderService orderService, OrderDetailService orderDetailService) {
        this.orderService = orderService;
        this.orderDetailService = orderDetailService;
    }

    // 1. [GET] /order/my-orders - Danh sách đơn hàng của user
    @GetMapping("/my-orders")
    public ResponseEntity<?> myOrders(HttpSession session) {
        User account = (User) session.getAttribute("account");
        if (account == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Not logged in"));
        }

        List<Order> orders = orderService.getOrdersByUserId(account.getUserId());
        List<Map<String, Object>> responseList = new java.util.ArrayList<>();
        for (Order order : orders) {
            Map<String, Object> orderMap = new HashMap<>();
            orderMap.put("orderId", order.getOrderId());
            orderMap.put("totalPrice", order.getTotalPrice());
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
        return ResponseEntity.ok(Map.of("message", "Đã hủy giao dịch của đơn hàng #" + order.getOrderId(), "status", "CANCELLED"));
    }
}