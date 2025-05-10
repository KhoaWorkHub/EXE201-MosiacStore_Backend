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
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Configuration
@EnableScheduling
@RequiredArgsConstructor
@Slf4j
public class AbandonedCartScheduler {

    private final CartRepository cartRepository;
    private final AdminNotificationService adminNotificationService;

    // Identify abandoned carts (run every 30 minutes)
    @Scheduled(cron = "0 */30 * * * ?")
    public void checkForAbandonedCarts() {
        LocalDateTime threeHoursAgo = LocalDateTime.now().minusHours(3);

        // Find carts that have items and haven't been updated in over 3 hours
        // This query needs to be added to CartRepository
        List<Cart> abandonedCarts = cartRepository.findCartsWithItemsNotUpdatedSince(threeHoursAgo);

        log.info("Found {} abandoned carts to notify admins about", abandonedCarts.size());

        // To avoid overwhelming the system, process in small batches if large number found
        if (abandonedCarts.size() > 10) {
            // Process in batches of 10
            for (int i = 0; i < abandonedCarts.size(); i += 10) {
                int endIndex = Math.min(i + 10, abandonedCarts.size());
                List<Cart> batch = abandonedCarts.subList(i, endIndex);

                List<CompletableFuture<Void>> futures = batch.stream()
                        .map(adminNotificationService::notifyAbandonedCart)
                        .collect(Collectors.toList());

                // Wait for all notifications in this batch to complete
                CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
            }
        } else {
            // Process all at once for smaller sets
            abandonedCarts.forEach(adminNotificationService::notifyAbandonedCart);
        }
    }
}