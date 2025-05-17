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
public class AverageOrderValueResponse {
    private List<TimeSeriesDataPoint> data;
    private BigDecimal overallAverageValue;
    private BigDecimal minAverageValue;
    private BigDecimal maxAverageValue;
    private double growthPercentage;
}