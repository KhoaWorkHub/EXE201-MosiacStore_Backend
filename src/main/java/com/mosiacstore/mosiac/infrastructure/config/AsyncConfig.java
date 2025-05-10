package com.mosiacstore.mosiac.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * Configuration for asynchronous task execution
 * Used for email sending and admin notifications to avoid blocking main request threads
 */
@Configuration
@EnableAsync
public class AsyncConfig {

    /**
     * Task executor for handling asynchronous operations
     * Optimized for I/O-bound tasks like sending emails and notifications
     */
    @Bean(name = "taskExecutor")
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        // Core pool size is the minimum number of workers to keep alive
        executor.setCorePoolSize(5);

        // Max pool size is the maximum number of workers that can be created
        executor.setMaxPoolSize(10);

        // Queue capacity is the number of tasks that can be queued if all threads are busy
        executor.setQueueCapacity(25);

        // Thread name prefix for better debugging
        executor.setThreadNamePrefix("MosiacAsync-");

        // Initialize the executor
        executor.initialize();

        return executor;
    }
}