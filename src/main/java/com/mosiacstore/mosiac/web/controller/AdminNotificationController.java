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

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/notifications")
@RequiredArgsConstructor
@Tag(name = "Admin Notifications", description = "Admin Notification Management API")
@PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
public class AdminNotificationController {

    private final NotificationService notificationService;

    @Operation(
            summary = "Get all ecommerce notification counts",
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @GetMapping("/counts")
    public ResponseEntity<Map<String, Long>> getNotificationCounts(
            @AuthenticationPrincipal CustomUserDetail currentUser) {
        Map<String, Long> counts = new HashMap<>();
        UUID userId = currentUser.getUser().getId();

        counts.put("newOrders", notificationService.countNotificationsByTypeAndUser(userId, NotificationType.NEW_ORDER));
        counts.put("cartActivities", notificationService.countNotificationsByTypeAndUser(userId, NotificationType.CART_ACTIVITY));
        counts.put("abandonedCarts", notificationService.countNotificationsByTypeAndUser(userId, NotificationType.ABANDONED_CART));
        counts.put("totalUnread", notificationService.countUnreadNotifications(userId));

        return ResponseEntity.ok(counts);
    }

    @Operation(
            summary = "Mark all notifications of a specific type as read",
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @PutMapping("/mark-type-as-read/{type}")
    public ResponseEntity<ApiResponse> markAllTypeAsRead(
            @PathVariable String type,
            @AuthenticationPrincipal CustomUserDetail currentUser) {
        try {
            NotificationType notificationType = NotificationType.valueOf(type.toUpperCase());
            notificationService.markAllTypeAsRead(currentUser.getUser().getId(), notificationType);
            return ResponseEntity.ok(new ApiResponse(true, "All " + type + " notifications marked as read"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(new ApiResponse(false, "Invalid notification type: " + type));
        }
    }

    @Operation(
            summary = "Get all order notifications",
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @GetMapping("/orders")
    public ResponseEntity<PageResponse<NotificationResponse>> getOrderNotifications(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(
                notificationService.getNotificationsByTypeOnly(NotificationType.NEW_ORDER, page, size)
        );
    }

    @Operation(
            summary = "Get all cart activity notifications",
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @GetMapping("/cart-activities")
    public ResponseEntity<PageResponse<NotificationResponse>> getCartActivityNotifications(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(
                notificationService.getNotificationsByTypeOnly(NotificationType.CART_ACTIVITY, page, size)
        );
    }

    @Operation(
            summary = "Get all abandoned cart notifications",
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @GetMapping("/abandoned-carts")
    public ResponseEntity<PageResponse<NotificationResponse>> getAbandonedCartNotifications(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(
                notificationService.getNotificationsByTypeOnly(NotificationType.ABANDONED_CART, page, size)
        );
    }
}