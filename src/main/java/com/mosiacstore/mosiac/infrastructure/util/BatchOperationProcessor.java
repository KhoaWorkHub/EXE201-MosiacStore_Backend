package com.mosiacstore.mosiac.infrastructure.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Utility for processing batches of operations efficiently
 * This is particularly useful for handling large numbers of notifications
 * or email sending operations
 */
@Component
@Slf4j
public class BatchOperationProcessor {

    /**
     * Process a collection in parallel batches
     *
     * @param items The collection to process
     * @param batchSize Size of each batch
     * @param operation Operation to perform on each item
     * @param <T> Type of items
     */
    public <T> void processBatch(Collection<T> items, int batchSize, Consumer<T> operation) {
        if (items == null || items.isEmpty()) {
            return;
        }

        // For small collections, process directly
        if (items.size() <= batchSize) {
            items.forEach(operation);
            return;
        }

        // For larger collections, use parallel processing with an executor
        ExecutorService executor = Executors.newFixedThreadPool(
                Math.min(10, Runtime.getRuntime().availableProcessors())
        );

        try {
            List<List<T>> batches = createBatches(items, batchSize);

            List<CompletableFuture<Void>> futures = batches.stream()
                    .map(batch -> CompletableFuture.runAsync(() -> {
                        for (T item : batch) {
                            try {
                                operation.accept(item);
                            } catch (Exception e) {
                                log.error("Error processing batch item: {}", e.getMessage(), e);
                            }
                        }
                    }, executor))
                    .collect(Collectors.toList());

            // Wait for all batches to complete
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        } finally {
            executor.shutdown();
        }
    }

    /**
     * Process items in parallel and collect results
     *
     * @param items Items to process
     * @param batchSize Size of each batch
     * @param operation Function to apply to each item
     * @param <T> Input type
     * @param <R> Result type
     * @return List of results
     */
    public <T, R> List<R> processBatchWithResults(Collection<T> items, int batchSize,
                                                  Function<T, R> operation) {
        if (items == null || items.isEmpty()) {
            return List.of();
        }

        // For small collections, process directly
        if (items.size() <= batchSize) {
            return items.stream()
                    .map(operation)
                    .collect(Collectors.toList());
        }

        // For larger collections, use parallel processing
        ExecutorService executor = Executors.newFixedThreadPool(
                Math.min(10, Runtime.getRuntime().availableProcessors())
        );

        try {
            List<List<T>> batches = createBatches(items, batchSize);

            List<CompletableFuture<List<R>>> futures = batches.stream()
                    .map(batch -> CompletableFuture.supplyAsync(() ->
                                    batch.stream()
                                            .map(item -> {
                                                try {
                                                    return operation.apply(item);
                                                } catch (Exception e) {
                                                    log.error("Error processing batch item: {}", e.getMessage(), e);
                                                    return null;
                                                }
                                            })
                                            .filter(result -> result != null)
                                            .collect(Collectors.toList()),
                            executor))
                    .collect(Collectors.toList());

            // Wait for all and collect results
            return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                    .thenApply(v -> futures.stream()
                            .map(CompletableFuture::join)
                            .flatMap(Collection::stream)
                            .collect(Collectors.toList()))
                    .join();

        } finally {
            executor.shutdown();
        }
    }

    /**
     * Split a collection into batches
     */
    private <T> List<List<T>> createBatches(Collection<T> items, int batchSize) {
        return items.stream()
                .collect(Collectors.groupingBy(item ->
                        Math.floor(items.stream().toList().indexOf(item) / (double) batchSize)))
                .values().stream()
                .map(batch -> batch.stream().collect(Collectors.toList()))
                .collect(Collectors.toList());
    }
}