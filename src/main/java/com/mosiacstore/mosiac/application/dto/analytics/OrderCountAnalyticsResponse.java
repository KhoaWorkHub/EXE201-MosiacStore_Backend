package com.mosiacstore.mosiac.application.dto.analytics;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderCountAnalyticsResponse {
    private List<TimeSeriesDataPoint> data;
    private long totalOrderCount;
    private double averageOrderCount;
    private long minOrderCount;
    private long maxOrderCount;
    private double growthPercentage;
}