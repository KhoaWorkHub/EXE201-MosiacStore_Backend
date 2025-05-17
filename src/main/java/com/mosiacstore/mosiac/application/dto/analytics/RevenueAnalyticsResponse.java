package com.mosiacstore.mosiac.application.dto.analytics;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RevenueAnalyticsResponse {
    private List<TimeSeriesDataPoint> data;
    private BigDecimal totalRevenue;
    private BigDecimal averageRevenue;
    private BigDecimal minRevenue;
    private BigDecimal maxRevenue;
    private double growthPercentage;
}