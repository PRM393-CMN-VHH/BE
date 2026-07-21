package prm393.group8.flowermanagement.service;

import prm393.group8.flowermanagement.entity.Order;
import org.springframework.data.domain.Page;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface OrderService {
    Order createOrder(Order order);
    Optional<Order> getOrderById(int id);
    List<Order> getOrdersByUserId(int userId);
    List<Order> getOrdersByUserIdAndStatus(int userId, String status);
    List<Order> getOrdersByUserIdAndStatuses(int userId, List<String> statuses);

    List<Order> getAll();
    Order updateOrderStatus(int orderId, String orderStatus, String paymentStatus);
    void updateOrderStatusById(int orderId, String newStatus);
    int countOrdersByStatus(String status);
    List<Order> findByUserEmailContainingIgnoreCase(String email);
    // Lấy tất cả đơn hàng phân trang
    Page<Order> getPaginatedOrders(int pageNo, int pageSize);
    // Tìm kiếm theo email + phân trang
    Page<Order> searchPaginatedOrdersByEmail(String email, int pageNo, int pageSize);
    public Page<Order> filterOrdersPaginated(String email, String status, String paymentStatus, LocalDate startDate, LocalDate endDate, int pageNo, int pageSize);

}