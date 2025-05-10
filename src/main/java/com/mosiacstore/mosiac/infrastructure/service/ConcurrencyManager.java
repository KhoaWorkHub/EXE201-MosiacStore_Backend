package com.mosiacstore.mosiac.infrastructure.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * Service to manage high-concurrency operations
 * Particularly useful for AWS t2.micro instances with limited resources
 */
@Service
@Slf4j
public class ConcurrencyManager {

    // Resource type to semaphore mapping
    private final ConcurrentHashMap<String, Semaphore> resourceLocks = new ConcurrentHashMap<>();

    // Default permits for different operations
    private static final int DEFAULT_CART_PERMITS = 20;
    private static final int DEFAULT_ORDER_PERMITS = 10;
    private static final int DEFAULT_EMAIL_PERMITS = 5;
    private static final int DEFAULT_NOTIFICATION_PERMITS = 15;

    /**
     * Execute an operation with concurrency control
     *
     * @param resourceType Type of resource being accessed
     * @param key Specific resource key
     * @param operation Operation to execute
     * @param <T> Return type of operation
     * @return Result of operation
     */
    public <T> T executeWithConcurrencyControl(
            String resourceType,
            String key,
            Supplier<T> operation) {

        String lockKey = resourceType + ":" + key;
        Semaphore semaphore = getOrCreateSemaphore(resourceType);

        try {
            // Try to acquire a permit with timeout
            if (!semaphore.tryAcquire(5, TimeUnit.SECONDS)) {
                log.warn("Failed to acquire lock for {}, proceeding without lock", lockKey);
                return operation.get();
            }

            // Execute operation with acquired permit
            return operation.get();

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Thread interrupted while waiting for lock on {}", lockKey, e);
            return operation.get();
        } finally {
            semaphore.release();
        }
    }

    /**
     * Execute an operation with concurrency control (void version)
     */
    public void executeWithConcurrencyControl(
            String resourceType,
            String key,
            Runnable operation) {

        executeWithConcurrencyControl(resourceType, key, () -> {
            operation.run();
            return null;
        });
    }

    /**
     * Get or create a semaphore for a resource type
     */
    private Semaphore getOrCreateSemaphore(String resourceType) {
        return resourceLocks.computeIfAbsent(resourceType, type -> {
            int permits = switch (type) {
                case "cart" -> DEFAULT_CART_PERMITS;
                case "order" -> DEFAULT_ORDER_PERMITS;
                case "email" -> DEFAULT_EMAIL_PERMITS;
                case "notification" -> DEFAULT_NOTIFICATION_PERMITS;
                default -> 10; // Default value
            };
            return new Semaphore(permits, true);
        });
    }
}