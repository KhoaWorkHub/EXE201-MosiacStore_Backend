package com.mosiacstore.mosiac.application.dto.analytics;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardStatsResponse {
    // Revenue stats
    private BigDecimal totalRevenue;
    private BigDecimal todayRevenue;
    private BigDecimal thisWeekRevenue;
    private BigDecimal thisMonthRevenue;
    private double revenueGrowth;

    // Order stats
    private long totalOrders;
    private long todayOrders;
    private long thisWeekOrders;
    private long thisMonthOrders;
    private double orderGrowth;

    // Customer stats
    private long totalCustomers;
    private long newCustomersToday;
    private long newCustomersThisWeek;
    private long newCustomersThisMonth;
    private double customerGrowth;

    // Order status breakdown
    private Map<String, Long> orderStatusCounts;

    // Average values
    private BigDecimal averageOrderValue;
    private double cartConversionRate;

    // Recent metrics
    private long pendingOrders;
    private long processingOrders;
    private long shippingOrders;
}