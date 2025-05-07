package com.mosiacstore.mosiac.infrastructure.config;

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
public class CartCleanupScheduler {

    private final CartRepository cartRepository;

    @Scheduled(cron = "0 0 1 * * ?") // Run at 1:00 AM every day
    public void cleanupExpiredCarts() {
        LocalDateTime now = LocalDateTime.now();
        List<Cart> expiredCarts = cartRepository.findExpiredCarts(now);

        log.info("Found {} expired carts to clean up", expiredCarts.size());
        cartRepository.deleteAll(expiredCarts);
        log.info("Expired carts cleanup completed");
    }
}