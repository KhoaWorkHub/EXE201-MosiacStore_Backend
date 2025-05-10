package com.mosiacstore.mosiac.application.dto.notification;

import com.mosiacstore.mosiac.domain.notification.NotificationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationResponse {
    private UUID id;
    private String title;
    private String content;
    private NotificationType type;
    private String sourceType;
    private String sourceId;
    private Boolean isRead;
    private String actionUrl;
    private LocalDateTime createdAt;
}