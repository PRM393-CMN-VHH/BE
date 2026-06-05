package prm393.group8.flowermanagement.repository;

import prm393.group8.flowermanagement.entity.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, Integer> {
    List<Order> findByUser_UserId(int userId);
    List<Order> findByUserEmailContainingIgnoreCase(String email);
    Page<Order> findByUserEmailContainingIgnoreCase(String email, Pageable pageable);
    List<Order> findByOrderStatus(String status);
    List<Order> findByCreatedAtBetween(LocalDate startDate, LocalDate endDate);
}