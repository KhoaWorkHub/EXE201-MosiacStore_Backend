package com.mosiacstore.mosiac.web.controller;

import com.mosiacstore.mosiac.application.dto.notification.NotificationResponse;
import com.mosiacstore.mosiac.application.dto.response.ApiResponse;
import com.mosiacstore.mosiac.application.dto.response.PageResponse;
import com.mosiacstore.mosiac.application.service.NotificationService;
import com.mosiacstore.mosiac.domain.notification.NotificationType;
import com.mosiacstore.mosiac.infrastructure.security.CustomUserDetail;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
@Tag(name = "Notifications", description = "Notifications API")
public class NotificationController {

    private final NotificationService notificationService;

    @Operation(
            summary = "Get user notifications",
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PageResponse<NotificationResponse>> getUserNotifications(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @AuthenticationPrincipal CustomUserDetail currentUser) {
        return ResponseEntity.ok(
                notificationService.getUserNotifications(currentUser.getUser().getId(), page, size)
        );
    }

    @Operation(
            summary = "Get unread notifications",
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @GetMapping("/unread")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PageResponse<NotificationResponse>> getUnreadNotifications(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @AuthenticationPrincipal CustomUserDetail currentUser) {
        return ResponseEntity.ok(
                notificationService.getUnreadNotifications(currentUser.getUser().getId(), page, size)
        );
    }

    @Operation(
            summary = "Get notifications by type",
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @GetMapping("/type/{type}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PageResponse<NotificationResponse>> getNotificationsByType(
            @PathVariable NotificationType type,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @AuthenticationPrincipal CustomUserDetail currentUser) {
        return ResponseEntity.ok(
                notificationService.getNotificationsByType(currentUser.getUser().getId(), type, page, size)
        );
    }

    @Operation(
            summary = "Count unread notifications",
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @GetMapping("/unread/count")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Long> countUnreadNotifications(
            @AuthenticationPrincipal CustomUserDetail currentUser) {
        return ResponseEntity.ok(
                notificationService.countUnreadNotifications(currentUser.getUser().getId())
        );
    }

    @Operation(
            summary = "Get notification by ID",
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @GetMapping("/{notificationId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<NotificationResponse> getNotificationById(
            @PathVariable UUID notificationId,
            @AuthenticationPrincipal CustomUserDetail currentUser) {
        return ResponseEntity.ok(
                notificationService.getNotificationById(notificationId, currentUser.getUser().getId())
        );
    }

    @Operation(
            summary = "Mark notification as read",
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @PutMapping("/{notificationId}/read")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse> markNotificationAsRead(
            @PathVariable UUID notificationId,
            @AuthenticationPrincipal CustomUserDetail currentUser) {
        notificationService.markAsRead(notificationId, currentUser.getUser().getId());
        return ResponseEntity.ok(new ApiResponse(true, "Notification marked as read"));
    }

    @Operation(
            summary = "Mark all notifications as read",
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @PutMapping("/read-all")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse> markAllNotificationsAsRead(
            @AuthenticationPrincipal CustomUserDetail currentUser) {
        notificationService.markAllAsRead(currentUser.getUser().getId());
        return ResponseEntity.ok(new ApiResponse(true, "All notifications marked as read"));
    }

    @Operation(
            summary = "Delete notification",
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @DeleteMapping("/{notificationId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse> deleteNotification(
            @PathVariable UUID notificationId,
            @AuthenticationPrincipal CustomUserDetail currentUser) {
        notificationService.deleteNotification(notificationId, currentUser.getUser().getId());
        return ResponseEntity.ok(new ApiResponse(true, "Notification deleted successfully"));
    }
}