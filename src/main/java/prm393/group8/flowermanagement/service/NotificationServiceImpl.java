package prm393.group8.flowermanagement.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import prm393.group8.flowermanagement.entity.AppNotification;
import prm393.group8.flowermanagement.entity.User;
import prm393.group8.flowermanagement.repository.AppNotificationRepository;
import prm393.group8.flowermanagement.repository.UserRepository;
import prm393.group8.flowermanagement.websocket.OrderWebSocketHandler;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class NotificationServiceImpl implements NotificationService {

    private final AppNotificationRepository notificationRepository;
    private final OrderWebSocketHandler orderWebSocketHandler;
    private final UserRepository userRepository;

    public NotificationServiceImpl(AppNotificationRepository notificationRepository,
                                    OrderWebSocketHandler orderWebSocketHandler,
                                    UserRepository userRepository) {
        this.notificationRepository = notificationRepository;
        this.orderWebSocketHandler = orderWebSocketHandler;
        this.userRepository = userRepository;
    }

    @Override
    public List<AppNotification> getNotificationsForUser(int userId) {
        return notificationRepository.findByUser_UserIdOrderByCreatedAtDesc(userId);
    }

    @Override
    public AppNotification notify(User user, String title, String content) {
        return notify(user, title, content, null);
    }

    @Override
    public AppNotification notify(User user, String title, String content, Integer orderId) {
        AppNotification saved = notificationRepository.save(new AppNotification(user, title, content, orderId));
        Map<String, Object> payload = new HashMap<>();
        payload.put("notificationId", saved.getNotificationId());
        payload.put("title", saved.getTitle());
        payload.put("content", saved.getContent());
        payload.put("createdAt", saved.getCreatedAt().toString());
        payload.put("read", saved.isRead());
        payload.put("orderId", saved.getOrderId());
        orderWebSocketHandler.pushToUser(user.getUserId(), "notification", payload);
        return saved;
    }

    @Override
    public void notifyAdmins(String title, String content, Integer orderId) {
        List<User> admins = userRepository.findByRole_RoleNameIgnoreCase("admin");
        for (User admin : admins) {
            notify(admin, title, content, orderId);
        }
    }

    @Override
    public void markAsRead(int notificationId, int userId) {
        AppNotification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy thông báo"));
        if (notification.getUser().getUserId() != userId) {
            throw new IllegalArgumentException("Không có quyền với thông báo này");
        }
        notification.setRead(true);
        notificationRepository.save(notification);
    }

    @Override
    @Transactional
    public void markAllAsRead(int userId) {
        notificationRepository.markAllReadForUser(userId);
    }
}
