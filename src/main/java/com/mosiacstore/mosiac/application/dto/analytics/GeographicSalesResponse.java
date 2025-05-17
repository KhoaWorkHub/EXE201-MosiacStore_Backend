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
public class GeographicSalesResponse {
    private String provinceCode;
    private String provinceName;
    private long orderCount;
    private BigDecimal revenue;
    private double percentageOfTotalOrders;
    private double percentageOfTotalRevenue;
}