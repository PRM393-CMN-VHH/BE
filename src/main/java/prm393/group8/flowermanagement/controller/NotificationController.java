package prm393.group8.flowermanagement.controller;

import prm393.group8.flowermanagement.entity.AppNotification;
import prm393.group8.flowermanagement.entity.User;
import prm393.group8.flowermanagement.service.NotificationService;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@CrossOrigin(origins = "*")
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    // [GET] /api/notifications - Danh sách thông báo của user hiện tại (mới nhất trước)
    @GetMapping("")
    public ResponseEntity<?> getMyNotifications(HttpSession session) {
        User user = currentUser(session);
        if (user == null) return ResponseEntity.status(401).body(Map.of("error", "Not logged in"));

        List<Map<String, Object>> notifications = notificationService
                .getNotificationsForUser(user.getUserId())
                .stream()
                .map(this::toDto)
                .toList();
        return ResponseEntity.ok(notifications);
    }

    // [POST] /api/notifications/{id}/read - Đánh dấu một thông báo đã đọc
    @PostMapping("/{id}/read")
    public ResponseEntity<?> markAsRead(@PathVariable("id") int id, HttpSession session) {
        User user = currentUser(session);
        if (user == null) return ResponseEntity.status(401).body(Map.of("error", "Not logged in"));

        notificationService.markAsRead(id, user.getUserId());
        return ResponseEntity.ok(Map.of("message", "OK"));
    }

    // [POST] /api/notifications/read-all - Đánh dấu tất cả đã đọc
    @PostMapping("/read-all")
    public ResponseEntity<?> markAllAsRead(HttpSession session) {
        User user = currentUser(session);
        if (user == null) return ResponseEntity.status(401).body(Map.of("error", "Not logged in"));

        notificationService.markAllAsRead(user.getUserId());
        return ResponseEntity.ok(Map.of("message", "OK"));
    }

    // Không trả entity trực tiếp để tránh lộ thông tin User (password...) qua JSON.
    private Map<String, Object> toDto(AppNotification notification) {
        return Map.of(
                "notificationId", notification.getNotificationId(),
                "title", notification.getTitle(),
                "content", notification.getContent(),
                "createdAt", notification.getCreatedAt().toString(),
                "read", notification.isRead()
        );
    }

    private User currentUser(HttpSession session) {
        User user = (User) session.getAttribute("account");
        if (user == null) user = (User) session.getAttribute("adminInfo");
        return user;
    }
}
