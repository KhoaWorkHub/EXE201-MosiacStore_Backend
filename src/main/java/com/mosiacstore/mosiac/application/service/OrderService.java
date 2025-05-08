package com.mosiacstore.mosiac.application.service;

import com.mosiacstore.mosiac.application.dto.request.CheckoutRequest;
import com.mosiacstore.mosiac.application.dto.response.CheckoutResponse;
import com.mosiacstore.mosiac.application.dto.response.OrderResponse;
import com.mosiacstore.mosiac.application.dto.response.PageResponse;

import java.time.LocalDateTime;
import java.util.UUID;

public interface OrderService {
    PageResponse<OrderResponse> getUserOrders(UUID userId, int page, int size, String sort);
    OrderResponse getOrderById(UUID id, UUID userId);
    OrderResponse getOrderByNumber(String orderNumber, UUID userId);
    CheckoutResponse checkout(CheckoutRequest request, UUID userId);
    OrderResponse cancelOrder(UUID id, String reason, UUID userId);

    // Admin functions
    PageResponse<OrderResponse> getAllOrders(String keyword, String status, UUID userId,
                                             LocalDateTime startDate, LocalDateTime endDate,
                                             int page, int size, String sort);
    OrderResponse updateOrderStatus(UUID id, String status, String adminNote, UUID adminId);
}