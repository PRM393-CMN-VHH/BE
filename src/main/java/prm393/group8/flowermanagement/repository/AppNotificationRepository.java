package prm393.group8.flowermanagement.repository;

import prm393.group8.flowermanagement.entity.AppNotification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AppNotificationRepository extends JpaRepository<AppNotification, Integer> {

    List<AppNotification> findByUser_UserIdOrderByCreatedAtDesc(int userId);

    @Modifying
    @Query("UPDATE AppNotification n SET n.read = true WHERE n.user.userId = :userId")
    int markAllReadForUser(@Param("userId") int userId);
}
