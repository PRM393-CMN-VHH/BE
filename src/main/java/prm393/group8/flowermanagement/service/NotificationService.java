package prm393.group8.flowermanagement.service;

import prm393.group8.flowermanagement.entity.AppNotification;
import prm393.group8.flowermanagement.entity.User;

import java.util.List;

public interface NotificationService {

    List<AppNotification> getNotificationsForUser(int userId);

    AppNotification notify(User user, String title, String content);

    void markAsRead(int notificationId, int userId);

    void markAllAsRead(int userId);
}
