package com.mosiacstore.mosiac.application.service.Impl;

import com.mosiacstore.mosiac.application.dto.notification.NotificationResponse;
import com.mosiacstore.mosiac.application.service.AdminNotificationService;
import com.mosiacstore.mosiac.application.service.NotificationService;
import com.mosiacstore.mosiac.domain.cart.Cart;
import com.mosiacstore.mosiac.domain.notification.NotificationType;
import com.mosiacstore.mosiac.domain.order.Order;
import com.mosiacstore.mosiac.domain.user.User;
import com.mosiacstore.mosiac.domain.user.UserRole;
import com.mosiacstore.mosiac.infrastructure.repository.CartRepository;
import com.mosiacstore.mosiac.infrastructure.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminNotificationServiceImpl implements AdminNotificationService {

    private final NotificationService notificationService;
    private final UserRepository userRepository;
    private final CartRepository cartRepository;
    private final SimpMessagingTemplate messagingTemplate;

    @Override
    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
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

    @Override
    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
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

    @Override
    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public CompletableFuture<Void> notifyAbandonedCart(UUID cartId) {
        try {
            // Reload the cart in this transaction context
            Cart cart = cartRepository.findById(cartId)
                    .orElseThrow(() -> new RuntimeException("Cart not found: " + cartId));

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
            log.error("Failed to send admin notification for abandoned cart: {}", cartId, e);
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