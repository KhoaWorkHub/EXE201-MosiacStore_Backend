package com.mosiacstore.mosiac.application.service.Impl;

import com.mosiacstore.mosiac.application.dto.notification.NotificationResponse;
import com.mosiacstore.mosiac.application.service.AdminNotificationService;
import com.mosiacstore.mosiac.application.service.NotificationService;
import com.mosiacstore.mosiac.domain.cart.Cart;
import com.mosiacstore.mosiac.domain.notification.NotificationType;
import com.mosiacstore.mosiac.domain.order.Order;
import com.mosiacstore.mosiac.domain.user.User;
import com.mosiacstore.mosiac.domain.user.UserRole;
import com.mosiacstore.mosiac.infrastructure.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Service for handling admin-specific notifications for order and cart events
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AdminNotificationServiceImpl implements AdminNotificationService {

    private final NotificationService notificationService;
    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Notify all admins about a new order
     * This method runs asynchronously to not block the main workflow
     */
    @Override
    @Async
    public CompletableFuture<Void> notifyNewOrder(Order order) {
        try {
            List<User> adminUsers = userRepository.findByRole(UserRole.ADMIN);

            for (User admin : adminUsers) {
                String title = "New Order Placed!";
                String content = String.format("Order #%s for %s placed. Total: %s VND",
                        order.getOrderNumber(),
                        order.getRecipientName(),
                        order.getTotalAmount());

                NotificationResponse notification = notificationService.createNotification(
                        admin.getId(),
                        title,
                        content,
                        NotificationType.NEW_ORDER,
                        "ORDER",
                        order.getId().toString(),
                        "/admin/orders/" + order.getId()
                );

                // Also send via WebSocket for real-time notification
                messagingTemplate.convertAndSendToUser(
                        admin.getEmail(),
                        "/queue/notifications",
                        notification
                );
            }

            // Also notify staff users
            List<User> staffUsers = userRepository.findByRole(UserRole.STAFF);
            for (User staff : staffUsers) {
                String title = "New Order Placed!";
                String content = String.format("Order #%s for %s placed. Total: %s VND",
                        order.getOrderNumber(),
                        order.getRecipientName(),
                        order.getTotalAmount());

                NotificationResponse notification = notificationService.createNotification(
                        staff.getId(),
                        title,
                        content,
                        NotificationType.NEW_ORDER,
                        "ORDER",
                        order.getId().toString(),
                        "/admin/orders/" + order.getId()
                );

                messagingTemplate.convertAndSendToUser(
                        staff.getEmail(),
                        "/queue/notifications",
                        notification
                );
            }

            // Send a global admin notification for the dashboard counter
            messagingTemplate.convertAndSend(
                    "/topic/admin/new-orders",
                    order.getId()
            );

            log.info("Admin notification sent for new order: {}", order.getOrderNumber());
            return CompletableFuture.completedFuture(null);
        } catch (Exception e) {
            log.error("Failed to send admin notification for new order: {}", order.getId(), e);
            return CompletableFuture.failedFuture(e);
        }
    }

    /**
     * Notify admins about cart activities
     * This is important for sales staff to proactively reach out to customers
     */
    @Override
    @Async
    public CompletableFuture<Void> notifyCartActivity(Cart cart, String activity, int itemCount) {
        try {
            List<User> salesUsers = userRepository.findByRole(UserRole.STAFF);
            String customerInfo = cart.getUser() != null ?
                    cart.getUser().getEmail() :
                    "Guest user (" + cart.getGuestId() + ")";

            for (User staff : salesUsers) {
                String title = "Cart Activity";
                String content = String.format("%s %s. Cart now has %d items worth %s VND",
                        customerInfo,
                        activity,
                        itemCount,
                        calculateCartTotal(cart));

                NotificationResponse notification = notificationService.createNotification(
                        staff.getId(),
                        title,
                        content,
                        NotificationType.CART_ACTIVITY,
                        "CART",
                        cart.getId().toString(),
                        "/admin/carts/active"
                );

                messagingTemplate.convertAndSendToUser(
                        staff.getEmail(),
                        "/queue/notifications",
                        notification
                );
            }

            log.info("Admin notification sent for cart activity: {}", cart.getId());
            return CompletableFuture.completedFuture(null);
        } catch (Exception e) {
            log.error("Failed to send admin notification for cart activity: {}", cart.getId(), e);
            return CompletableFuture.failedFuture(e);
        }
    }

    /**
     * Notify admins about abandoned carts
     * This is scheduled to run periodically to identify inactive carts
     */
    @Override
    @Async
    public CompletableFuture<Void> notifyAbandonedCart(Cart cart) {
        try {
            List<User> salesUsers = userRepository.findByRole(UserRole.STAFF);
            String customerInfo = cart.getUser() != null ?
                    cart.getUser().getEmail() :
                    "Guest user (" + cart.getGuestId() + ")";

            for (User staff : salesUsers) {
                String title = "⚠️ Abandoned Cart Alert";
                String content = String.format("%s has items in cart worth %s VND for over 3 hours!",
                        customerInfo,
                        calculateCartTotal(cart));

                NotificationResponse notification = notificationService.createNotification(
                        staff.getId(),
                        title,
                        content,
                        NotificationType.ABANDONED_CART,
                        "CART",
                        cart.getId().toString(),
                        "/admin/carts/abandoned"
                );

                messagingTemplate.convertAndSendToUser(
                        staff.getEmail(),
                        "/queue/notifications",
                        notification
                );
            }

            log.info("Admin notification sent for abandoned cart: {}", cart.getId());
            return CompletableFuture.completedFuture(null);
        } catch (Exception e) {
            log.error("Failed to send admin notification for abandoned cart: {}", cart.getId(), e);
            return CompletableFuture.failedFuture(e);
        }
    }

    private String calculateCartTotal(Cart cart) {
        return cart.getItems().stream()
                .map(item -> item.getPriceSnapshot().multiply(new java.math.BigDecimal(item.getQuantity())))
                .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add)
                .toString();
    }
}