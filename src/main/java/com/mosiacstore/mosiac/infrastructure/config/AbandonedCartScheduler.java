package com.mosiacstore.mosiac.infrastructure.config;

import com.mosiacstore.mosiac.application.service.AdminNotificationService;
import com.mosiacstore.mosiac.domain.cart.Cart;
import com.mosiacstore.mosiac.infrastructure.repository.CartRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import java.time.LocalDateTime;
import java.util.List;

@Configuration
@EnableScheduling
@RequiredArgsConstructor
@Slf4j
public class AbandonedCartScheduler {

    private final CartRepository cartRepository;
    private final AdminNotificationService adminNotificationService;

    @Scheduled(fixedDelay = 30000)
    public void checkForAbandonedCarts() {
        //LocalDateTime abandonedTime = LocalDateTime.now().minusHours(3);
        LocalDateTime abandonedTime = LocalDateTime.now().minusSeconds(10);
        // Find carts that have items and haven't been updated in over 3 hours
        List<Cart> abandonedCarts = cartRepository.findCartsWithItemsNotUpdatedSince(abandonedTime);

        log.info("Found {} abandoned carts to notify admins about", abandonedCarts.size());

        // Just pass the IDs to avoid lazy loading issues
        for (Cart cart : abandonedCarts) {
            adminNotificationService.notifyAbandonedCart(cart.getId());
        }
    }
}