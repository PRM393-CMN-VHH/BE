package prm393.group8.flowermanagement.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import prm393.group8.flowermanagement.entity.AppNotification;
import prm393.group8.flowermanagement.entity.User;
import prm393.group8.flowermanagement.repository.AppNotificationRepository;

import java.util.List;

@Service
public class NotificationServiceImpl implements NotificationService {

    private final AppNotificationRepository notificationRepository;

    public NotificationServiceImpl(AppNotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    @Override
    public List<AppNotification> getNotificationsForUser(int userId) {
        return notificationRepository.findByUser_UserIdOrderByCreatedAtDesc(userId);
    }

    @Override
    public AppNotification notify(User user, String title, String content) {
        return notificationRepository.save(new AppNotification(user, title, content));
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
