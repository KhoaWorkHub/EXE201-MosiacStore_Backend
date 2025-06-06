package com.mosiacstore.mosiac.infrastructure.repository;

import com.mosiacstore.mosiac.domain.notification.Notification;
import com.mosiacstore.mosiac.domain.notification.NotificationType;
import com.mosiacstore.mosiac.domain.user.UserRole;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    Page<Notification> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    Page<Notification> findByUserIdAndIsReadOrderByCreatedAtDesc(UUID userId, Boolean isRead, Pageable pageable);

    Page<Notification> findByUserIdAndTypeOrderByCreatedAtDesc(UUID userId, NotificationType type, Pageable pageable);

    @Query("SELECT COUNT(n) FROM Notification n WHERE n.user.id = :userId AND n.isRead = false")
    long countUnreadByUserId(@Param("userId") UUID userId);

    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true WHERE n.user.id = :userId")
    void markAllAsRead(@Param("userId") UUID userId);

    long countByUserIdAndType(UUID userId, NotificationType type);

    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true WHERE n.user.id = :userId AND n.type = :type")
    void markAllAsReadByTypeAndUserId(@Param("userId") UUID userId, @Param("type") NotificationType type);

    @Query("SELECT n FROM Notification n WHERE n.type = :type")
    Page<Notification> findByType(@Param("type") NotificationType type, Pageable pageable);
}