package prm393.group8.flowermanagement.controller;

import prm393.group8.flowermanagement.entity.Order;
import prm393.group8.flowermanagement.entity.User;
import prm393.group8.flowermanagement.service.OrderService;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@CrossOrigin(origins = "*")
@RequestMapping("/transaction")
public class TransactionController {

    private final OrderService orderService;

    public TransactionController(OrderService orderService) {
        this.orderService = orderService;
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

        return ResponseEntity.ok(successfulOrders);
    }
}
