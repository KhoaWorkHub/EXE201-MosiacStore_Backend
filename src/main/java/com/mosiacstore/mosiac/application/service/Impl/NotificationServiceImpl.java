package com.mosiacstore.mosiac.application.service.Impl;

import com.mosiacstore.mosiac.application.dto.notification.NotificationResponse;
import com.mosiacstore.mosiac.application.dto.response.PageResponse;
import com.mosiacstore.mosiac.application.exception.EntityNotFoundException;
import com.mosiacstore.mosiac.application.exception.InvalidOperationException;
import com.mosiacstore.mosiac.application.service.NotificationService;
import com.mosiacstore.mosiac.domain.notification.Notification;
import com.mosiacstore.mosiac.domain.notification.NotificationType;
import com.mosiacstore.mosiac.domain.user.User;
import com.mosiacstore.mosiac.domain.user.UserRole;
import com.mosiacstore.mosiac.infrastructure.repository.NotificationRepository;
import com.mosiacstore.mosiac.infrastructure.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;

    @Override
    @Transactional
    public NotificationResponse createNotification(UUID userId, String title, String content,
                                                   NotificationType type, String sourceType,
                                                   String sourceId, String actionUrl) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found with ID: " + userId));

        Notification notification = new Notification();
        notification.setUser(user);
        notification.setTitle(title);
        notification.setContent(content);
        notification.setType(type);
        notification.setSourceType(sourceType);
        notification.setSourceId(sourceId);
        notification.setIsRead(false);
        notification.setActionUrl(actionUrl);
        notification.setCreatedAt(LocalDateTime.now());
        notification.setUpdatedAt(LocalDateTime.now());

        Notification savedNotification = notificationRepository.save(notification);

        NotificationResponse response = mapToNotificationResponse(savedNotification);

        // Send notification through WebSocket
        messagingTemplate.convertAndSendToUser(
                user.getEmail(),
                "/queue/notifications",
                response
        );

        return response;
    }

    @Override
    public NotificationResponse getNotificationById(UUID notificationId, UUID userId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new EntityNotFoundException("Notification not found with ID: " + notificationId));

        // Verify user owns the notification
        if (!notification.getUser().getId().equals(userId)) {
            throw new InvalidOperationException("User does not have access to this notification");
        }

        return mapToNotificationResponse(notification);
    }

    @Override
    public PageResponse<NotificationResponse> getUserNotifications(UUID userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Notification> notificationPage = notificationRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);

        List<NotificationResponse> notificationResponses = notificationPage.getContent().stream()
                .map(this::mapToNotificationResponse)
                .collect(Collectors.toList());

        return new PageResponse<>(
                notificationResponses,
                notificationPage.getNumber(),
                notificationPage.getSize(),
                notificationPage.getTotalElements(),
                notificationPage.getTotalPages(),
                notificationPage.isFirst(),
                notificationPage.isLast()
        );
    }

    @Override
    public PageResponse<NotificationResponse> getUnreadNotifications(UUID userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Notification> notificationPage = notificationRepository.findByUserIdAndIsReadOrderByCreatedAtDesc(userId, false, pageable);

        List<NotificationResponse> notificationResponses = notificationPage.getContent().stream()
                .map(this::mapToNotificationResponse)
                .collect(Collectors.toList());

        return new PageResponse<>(
                notificationResponses,
                notificationPage.getNumber(),
                notificationPage.getSize(),
                notificationPage.getTotalElements(),
                notificationPage.getTotalPages(),
                notificationPage.isFirst(),
                notificationPage.isLast()
        );
    }

    @Override
    public PageResponse<NotificationResponse> getNotificationsByType(UUID userId, NotificationType type, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Notification> notificationPage = notificationRepository.findByUserIdAndTypeOrderByCreatedAtDesc(userId, type, pageable);

        List<NotificationResponse> notificationResponses = notificationPage.getContent().stream()
                .map(this::mapToNotificationResponse)
                .collect(Collectors.toList());

        return new PageResponse<>(
                notificationResponses,
                notificationPage.getNumber(),
                notificationPage.getSize(),
                notificationPage.getTotalElements(),
                notificationPage.getTotalPages(),
                notificationPage.isFirst(),
                notificationPage.isLast()
        );
    }

    @Override
    @Transactional
    public void markAsRead(UUID notificationId, UUID userId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new EntityNotFoundException("Notification not found with ID: " + notificationId));

        // Verify user owns the notification
        if (!notification.getUser().getId().equals(userId)) {
            throw new InvalidOperationException("User does not have access to this notification");
        }

        notification.setIsRead(true);
        notification.setUpdatedAt(LocalDateTime.now());
        notificationRepository.save(notification);

        // Send update through WebSocket
        messagingTemplate.convertAndSendToUser(
                notification.getUser().getEmail(),
                "/queue/notifications/updated",
                mapToNotificationResponse(notification)
        );

        // Send unread count
        long unreadCount = notificationRepository.countUnreadByUserId(userId);
        messagingTemplate.convertAndSendToUser(
                notification.getUser().getEmail(),
                "/queue/notifications/count",
                unreadCount
        );
    }

    @Override
    @Transactional
    public void markAllAsRead(UUID userId) {
        notificationRepository.markAllAsRead(userId);

        // Send unread count (which will be 0)
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found with ID: " + userId));

        messagingTemplate.convertAndSendToUser(
                user.getEmail(),
                "/queue/notifications/count",
                0L
        );
    }

    @Override
    @Transactional
    public void deleteNotification(UUID notificationId, UUID userId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new EntityNotFoundException("Notification not found with ID: " + notificationId));

        // Verify user owns the notification
        if (!notification.getUser().getId().equals(userId)) {
            throw new InvalidOperationException("User does not have access to this notification");
        }

        notificationRepository.delete(notification);

        // Send unread count
        long unreadCount = notificationRepository.countUnreadByUserId(userId);
        messagingTemplate.convertAndSendToUser(
                notification.getUser().getEmail(),
                "/queue/notifications/count",
                unreadCount
        );
    }

    @Override
    public long countUnreadNotifications(UUID userId) {
        return notificationRepository.countUnreadByUserId(userId);
    }

    @Override
    @Transactional
    public void sendChatNotification(UUID userId, UUID roomId, UUID messageId, String senderName, String messagePreview) {
        String title = "New message from " + senderName;
        String content = messagePreview;

        createNotification(
                userId,
                title,
                content,
                NotificationType.CHAT_MESSAGE,
                "CHAT_MESSAGE",
                messageId.toString(),
                "/chat/rooms/" + roomId
        );
    }

    private NotificationResponse mapToNotificationResponse(Notification notification) {
        return NotificationResponse.builder()
                .id(notification.getId())
                .title(notification.getTitle())
                .content(notification.getContent())
                .type(notification.getType())
                .sourceType(notification.getSourceType())
                .sourceId(notification.getSourceId())
                .isRead(notification.getIsRead())
                .actionUrl(notification.getActionUrl())
                .createdAt(notification.getCreatedAt())
                .build();
    }

    @Override
    public long countNotificationsByTypeAndUser(UUID userId, NotificationType type) {
        return notificationRepository.countByUserIdAndType(userId, type);
    }

    @Override
    @Transactional
    public void markAllTypeAsRead(UUID userId, NotificationType type) {
        notificationRepository.markAllAsReadByTypeAndUserId(userId, type);
    }

    @Override
    public PageResponse<NotificationResponse> getNotificationsByTypeOnly(NotificationType type, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Notification> notificationPage = notificationRepository.findByType(type, pageable);

        List<NotificationResponse> notificationResponses = notificationPage.getContent().stream()
                .map(this::mapToNotificationResponse)
                .collect(Collectors.toList());

        return new PageResponse<>(
                notificationResponses,
                notificationPage.getNumber(),
                notificationPage.getSize(),
                notificationPage.getTotalElements(),
                notificationPage.getTotalPages(),
                notificationPage.isFirst(),
                notificationPage.isLast()
        );
    }
}