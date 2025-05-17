package com.mosiacstore.mosiac.web.controller;

import com.mosiacstore.mosiac.application.dto.analytics.*;
import com.mosiacstore.mosiac.application.service.OrderAnalyticsService;
import com.mosiacstore.mosiac.infrastructure.security.CustomUserDetail;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/analytics/orders")
@RequiredArgsConstructor
@Tag(name = "Order Analytics", description = "Order Analytics API")
@PreAuthorize("hasRole('ADMIN')")
public class OrderAnalyticsController {

    private final OrderAnalyticsService orderAnalyticsService;

    @Operation(
            summary = "Get revenue analytics",
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @GetMapping("/revenue")
    public ResponseEntity<RevenueAnalyticsResponse> getRevenueAnalytics(
            @RequestParam(required = false, defaultValue = "daily") String period,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @AuthenticationPrincipal CustomUserDetail currentUser) {
        return ResponseEntity.ok(orderAnalyticsService.getRevenueAnalytics(period, startDate, endDate));
    }

    @Operation(
            summary = "Get order count analytics",
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @GetMapping("/count")
    public ResponseEntity<OrderCountAnalyticsResponse> getOrderCountAnalytics(
            @RequestParam(required = false, defaultValue = "daily") String period,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @AuthenticationPrincipal CustomUserDetail currentUser) {
        return ResponseEntity.ok(orderAnalyticsService.getOrderCountAnalytics(period, startDate, endDate));
    }

    @Operation(
            summary = "Get average order value analytics",
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @GetMapping("/average-value")
    public ResponseEntity<AverageOrderValueResponse> getAverageOrderValue(
            @RequestParam(required = false, defaultValue = "daily") String period,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @AuthenticationPrincipal CustomUserDetail currentUser) {
        return ResponseEntity.ok(orderAnalyticsService.getAverageOrderValue(period, startDate, endDate));
    }

    @Operation(
            summary = "Get top-selling products",
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @GetMapping("/top-products")
    public ResponseEntity<List<TopProductResponse>> getTopProducts(
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @AuthenticationPrincipal CustomUserDetail currentUser) {
        return ResponseEntity.ok(orderAnalyticsService.getTopProducts(limit, startDate, endDate));
    }

    @Operation(
            summary = "Get geographic sales distribution",
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @GetMapping("/geographic")
    public ResponseEntity<List<GeographicSalesResponse>> getGeographicSales(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @AuthenticationPrincipal CustomUserDetail currentUser) {
        return ResponseEntity.ok(orderAnalyticsService.getGeographicSales(startDate, endDate));
    }

    @Operation(
            summary = "Get payment method breakdown",
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @GetMapping("/payment-methods")
    public ResponseEntity<List<PaymentMethodBreakdownResponse>> getPaymentMethodBreakdown(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @AuthenticationPrincipal CustomUserDetail currentUser) {
        return ResponseEntity.ok(orderAnalyticsService.getPaymentMethodBreakdown(startDate, endDate));
    }

    @Operation(
            summary = "Get dashboard statistics",
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @GetMapping("/dashboard")
    public ResponseEntity<DashboardStatsResponse> getDashboardStats(
            @AuthenticationPrincipal CustomUserDetail currentUser) {
        return ResponseEntity.ok(orderAnalyticsService.getDashboardStats());
    }
}