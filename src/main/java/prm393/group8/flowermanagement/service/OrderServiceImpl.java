package prm393.group8.flowermanagement.service;

import prm393.group8.flowermanagement.entity.Order;
import prm393.group8.flowermanagement.repository.OrderRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;

    public OrderServiceImpl(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    @Override
    public Order createOrder(Order order) {
        boolean isNewOrder = order.getOrderId() == 0;

        if (isNewOrder) {
            order.setCreatedAt(LocalDateTime.now());
            if (order.getOrderStatus() == null || order.getOrderStatus().isBlank()) {
                order.setOrderStatus("Pending");
            }
            if (order.getPaymentStatus() == null || order.getPaymentStatus().isBlank()) {
                order.setPaymentStatus("Unpaid");
            }
        }

        return orderRepository.save(order);
    }

    @Override
    public Optional<Order> getOrderById(int id) {
        return orderRepository.findById(id);
    }

    @Override
    public List<Order> getOrdersByUserId(int userId) {
        return orderRepository.findByUser_UserId(userId);
    }

    @Override
    public List<Order> getOrdersByUserIdAndStatus(int userId, String status) {
        return orderRepository.findByUser_UserIdAndOrderStatusIgnoreCase(userId, status);
    }

    @Override
    public List<Order> getOrdersByUserIdAndStatuses(int userId, List<String> statuses) {
        var wanted = statuses.stream().map(String::toUpperCase).collect(Collectors.toSet());
        return getOrdersByUserId(userId).stream()
                .filter(o -> wanted.contains(o.getOrderStatus().toUpperCase()))
                .toList();
    }

    @Override
    public List<Order> getAll() {
        return orderRepository.findAll();
    }

    @Override
    public Order updateOrderStatus(int orderId, String orderStatus, String paymentStatus) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn hàng với ID: " + orderId));
        order.setOrderStatus(orderStatus);
        order.setPaymentStatus(paymentStatus);
        return orderRepository.save(order);
    }

    @Override
    public void updateOrderStatusById(int orderId, String newStatus) {
        Order order = orderRepository.findById(orderId).orElse(null);
        if (order != null) {
            order.setOrderStatus(newStatus);
            orderRepository.save(order);
        }
    }

    @Override
    public int countOrdersByStatus(String status) {
        return (int) orderRepository.findAll()
                .stream()
                .filter(o -> o.getOrderStatus().equalsIgnoreCase(status))
                .count();
    }

    @Override
    public List<Order> findByUserEmailContainingIgnoreCase(String email) {
        return orderRepository.findAll()
                .stream()
                .filter(order -> order.getUser().getEmail().toLowerCase().contains(email.toLowerCase()))
                .collect(Collectors.toList());
    }

    @Override
    public Page<Order> getPaginatedOrders(int pageNo, int pageSize) {
        Pageable pageable = PageRequest.of(pageNo - 1, pageSize);
        return orderRepository.findAll(pageable);
    }

    @Override
    public Page<Order> searchPaginatedOrdersByEmail(String email, int pageNo, int pageSize) {
        Pageable pageable = PageRequest.of(pageNo - 1, pageSize);
        return orderRepository.findByUserEmailContainingIgnoreCase(email, pageable);
    }

    @Override
    public Page<Order> filterOrdersPaginated(String email, String status, String paymentStatus, LocalDate startDate, LocalDate endDate, int pageNo, int pageSize) {
        List<Order> orders = orderRepository.findAll();

        // Lọc theo email
        if (email != null && !email.isBlank()) {
            orders = orders.stream()
                    .filter(o -> o.getUser().getEmail().toLowerCase().contains(email.toLowerCase()))
                    .toList();
        }

        // Lọc theo trạng thái
        if (status != null && !status.isBlank()) {
            orders = orders.stream()
                    .filter(o -> o.getOrderStatus().equalsIgnoreCase(status))
                    .toList();
        }

        // Lọc theo trạng thái thanh toán
        if (paymentStatus != null && !paymentStatus.isBlank()) {
            orders = orders.stream()
                    .filter(o -> o.getPaymentStatus().equalsIgnoreCase(paymentStatus))
                    .toList();
        }

        // Lọc theo khoảng ngày
        if (startDate != null && endDate != null) {
            orders = orders.stream()
                    .filter(o -> {
                        LocalDate created = o.getCreatedAt().toLocalDate();
                        return !created.isBefore(startDate) && !created.isAfter(endDate);
                    })
                    .toList();
        }

        // Phân trang thủ công
        int start = (pageNo - 1) * pageSize;
        int end = Math.min(start + pageSize, orders.size());
        List<Order> pageContent = orders.subList(Math.max(0, start), end);

        return new PageImpl<>(pageContent, PageRequest.of(pageNo - 1, pageSize), orders.size());
    }

}