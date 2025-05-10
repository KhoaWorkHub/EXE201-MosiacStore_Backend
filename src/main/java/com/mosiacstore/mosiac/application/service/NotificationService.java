package com.mosiacstore.mosiac.application.service;

import com.mosiacstore.mosiac.application.dto.notification.NotificationResponse;
import com.mosiacstore.mosiac.application.dto.response.PageResponse;
import com.mosiacstore.mosiac.domain.notification.NotificationType;

import java.util.UUID;

public interface NotificationService {

    NotificationResponse createNotification(UUID userId, String title, String content,
                                            NotificationType type, String sourceType,
                                            String sourceId, String actionUrl);

    NotificationResponse getNotificationById(UUID notificationId, UUID userId);

    PageResponse<NotificationResponse> getUserNotifications(UUID userId, int page, int size);

    PageResponse<NotificationResponse> getUnreadNotifications(UUID userId, int page, int size);

    PageResponse<NotificationResponse> getNotificationsByType(UUID userId, NotificationType type, int page, int size);

    void markAsRead(UUID notificationId, UUID userId);

    void markAllAsRead(UUID userId);

    void deleteNotification(UUID notificationId, UUID userId);

    long countUnreadNotifications(UUID userId);

    void sendChatNotification(UUID userId, UUID roomId, UUID messageId, String senderName, String messagePreview);

    long countNotificationsByTypeAndUser(UUID userId, NotificationType type);

    void markAllTypeAsRead(UUID userId, NotificationType type);

    PageResponse<NotificationResponse> getNotificationsByTypeOnly(NotificationType type, int page, int size);

}