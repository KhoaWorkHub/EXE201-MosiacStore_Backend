package com.mosiacstore.mosiac.application.dto.analytics;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TimeSeriesDataPoint {
    private String label; // e.g., "2023-01-01" or "Week 1" or "January"
    private BigDecimal value;
    private long count;
}