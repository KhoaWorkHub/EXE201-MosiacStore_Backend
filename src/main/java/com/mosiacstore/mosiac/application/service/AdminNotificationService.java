package com.mosiacstore.mosiac.application.service;

import com.mosiacstore.mosiac.domain.cart.Cart;
import com.mosiacstore.mosiac.domain.order.Order;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface AdminNotificationService {
    CompletableFuture<Void> notifyNewOrder(Order order);
    CompletableFuture<Void> notifyCartActivity(Cart cart, String activity, int itemCount);
    CompletableFuture<Void> notifyAbandonedCart(UUID cartId);
}