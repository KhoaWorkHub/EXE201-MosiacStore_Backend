package com.mosiacstore.mosiac.application.service;

import com.mosiacstore.mosiac.application.dto.request.CheckoutRequest;
import com.mosiacstore.mosiac.application.dto.request.OrderItemRequest;
import com.mosiacstore.mosiac.application.dto.request.UpdateOrderItemsRequest;
import com.mosiacstore.mosiac.application.dto.response.CheckoutResponse;
import com.mosiacstore.mosiac.application.dto.response.OrderDetailResponse;
import com.mosiacstore.mosiac.application.dto.response.OrderResponse;
import com.mosiacstore.mosiac.application.dto.response.PageResponse;
import org.springframework.core.io.Resource;

import java.time.LocalDateTime;
import java.util.UUID;

public interface OrderService {
    PageResponse<OrderResponse> getUserOrders(UUID userId, int page, int size, String sort);
    OrderResponse getOrderById(UUID id, UUID userId);
    OrderResponse getOrderByNumber(String orderNumber, UUID userId);
    CheckoutResponse checkout(CheckoutRequest request, UUID userId);
    OrderResponse cancelOrder(UUID id, String reason, UUID userId);
    PageResponse<OrderResponse> getAllOrders(String keyword, String status, UUID userId,
                                             LocalDateTime startDate, LocalDateTime endDate,
                                             int page, int size, String sort);
    OrderResponse updateOrderStatus(UUID id, String status, String adminNote, UUID adminId);


    /**
     * Get detailed order information for admin
     */
    OrderDetailResponse getOrderDetails(UUID id);

    /**
     * Update order items (admin only)
     */
    OrderResponse updateOrderItems(UUID id, UpdateOrderItemsRequest request, UUID adminId);

    /**
     * Add item to order (admin only)
     */
    OrderResponse addOrderItem(UUID id, OrderItemRequest request, UUID adminId);

    /**
     * Remove item from order (admin only)
     */
    OrderResponse removeOrderItem(UUID id, UUID itemId, UUID adminId);

    /**
     * Export orders to file (CSV, Excel, etc.)
     */
    Resource exportOrders(String format, String status, LocalDateTime startDate, LocalDateTime endDate);
}