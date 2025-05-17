package com.mosiacstore.mosiac.application.service;

import com.mosiacstore.mosiac.application.dto.analytics.*;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public interface OrderAnalyticsService {

    /**
     * Get revenue analytics for the specified period
     */
    RevenueAnalyticsResponse getRevenueAnalytics(String period, LocalDateTime startDate, LocalDateTime endDate);

    /**
     * Get order count analytics for the specified period
     */
    OrderCountAnalyticsResponse getOrderCountAnalytics(String period, LocalDateTime startDate, LocalDateTime endDate);

    /**
     * Get average order value analytics for the specified period
     */
    AverageOrderValueResponse getAverageOrderValue(String period, LocalDateTime startDate, LocalDateTime endDate);

    /**
     * Get top-selling products for the specified period
     */
    List<TopProductResponse> getTopProducts(int limit, LocalDateTime startDate, LocalDateTime endDate);

    /**
     * Get geographic sales distribution for the specified period
     */
    List<GeographicSalesResponse> getGeographicSales(LocalDateTime startDate, LocalDateTime endDate);

    /**
     * Get payment method breakdown for the specified period
     */
    List<PaymentMethodBreakdownResponse> getPaymentMethodBreakdown(LocalDateTime startDate, LocalDateTime endDate);

    /**
     * Get dashboard statistics
     */
    DashboardStatsResponse getDashboardStats();
}