package prm393.group8.flowermanagement.service;

import prm393.group8.flowermanagement.entity.AppNotification;
import prm393.group8.flowermanagement.entity.User;

import java.util.List;

public interface NotificationService {

    List<AppNotification> getNotificationsForUser(int userId);

    AppNotification notify(User user, String title, String content);

    AppNotification notify(User user, String title, String content, Integer orderId);

    // Notifies every admin account (e.g. new order placed, customer confirmed receipt).
    void notifyAdmins(String title, String content, Integer orderId);

    void markAsRead(int notificationId, int userId);

    void markAllAsRead(int userId);
}
