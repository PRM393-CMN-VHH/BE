package prm393.group8.flowermanagement.controller;

import prm393.group8.flowermanagement.entity.Order;
import prm393.group8.flowermanagement.entity.OrderDetail;
import prm393.group8.flowermanagement.entity.User;
import prm393.group8.flowermanagement.service.OrderService;
import prm393.group8.flowermanagement.service.OrderDetailService;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.stream.Collectors;

@RestController
@CrossOrigin(origins = "*")
@RequestMapping("/transaction")
public class TransactionController {

    private final OrderService orderService;
    private final OrderDetailService orderDetailService;

    public TransactionController(OrderService orderService, OrderDetailService orderDetailService) {
        this.orderService = orderService;
        this.orderDetailService = orderDetailService;
    }

    // [GET] /transaction/history - Lấy lịch sử giao dịch
    @GetMapping("/history")
    public ResponseEntity<?> viewTransactionHistory(HttpSession session) {
        User account = (User) session.getAttribute("account");
        if (account == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Not logged in"));
        }

        List<Order> orders = orderService.getOrdersByUserId(account.getUserId());
        List<Order> successfulOrders = orders.stream()
                .filter(order -> "Paid".equalsIgnoreCase(order.getPaymentStatus()))
                .collect(Collectors.toList());

        List<Map<String, Object>> responseList = new java.util.ArrayList<>();
        for (Order order : successfulOrders) {
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
}
