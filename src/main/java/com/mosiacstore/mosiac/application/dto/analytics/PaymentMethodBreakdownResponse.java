package com.mosiacstore.mosiac.application.dto.analytics;

import com.mosiacstore.mosiac.domain.payment.PaymentMethod;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentMethodBreakdownResponse {
    private PaymentMethod paymentMethod;
    private String displayName;
    private long orderCount;
    private BigDecimal revenue;
    private double percentageOfTotalOrders;
    private double percentageOfTotalRevenue;
}