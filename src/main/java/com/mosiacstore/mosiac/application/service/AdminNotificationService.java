package com.mosiacstore.mosiac.application.service;

import com.mosiacstore.mosiac.domain.cart.Cart;
import com.mosiacstore.mosiac.domain.order.Order;

import java.util.concurrent.CompletableFuture;

/**
 * Interface for admin notification related operations.
 */
public interface AdminNotificationService {

    /**
     * Notify all admins and staff about a newly placed order.
     *
     * @param order the order that was placed
     * @return a CompletableFuture indicating the completion of the operation
     */
    CompletableFuture<Void> notifyNewOrder(Order order);

    /**
     * Notify staff users about customer cart activity.
     *
     * @param cart the cart where the activity occurred
     * @param activity the type of activity (e.g., "added item", "removed item")
     * @param itemCount total items in the cart after the activity
     * @return a CompletableFuture indicating the completion of the operation
     */
    CompletableFuture<Void> notifyCartActivity(Cart cart, String activity, int itemCount);

    /**
     * Notify admins/staff about an abandoned cart (user left without checkout).
     *
     * @param cart the abandoned cart
     * @return a CompletableFuture representing the async process
     */
    CompletableFuture<Void> notifyAbandonedCart(Cart cart);
}
