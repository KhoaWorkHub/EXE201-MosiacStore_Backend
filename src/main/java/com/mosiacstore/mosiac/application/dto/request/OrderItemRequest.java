package com.mosiacstore.mosiac.application.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderItemRequest {
    private UUID productId;
    private UUID variantId;
    private Integer quantity;
    private BigDecimal priceOverride;
}