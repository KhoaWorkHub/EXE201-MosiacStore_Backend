package com.mosiacstore.mosiac.application.dto.analytics;

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
public class TopProductResponse {
    private UUID productId;
    private String productName;
    private String productSlug;
    private String productImage;
    private int quantity;
    private BigDecimal revenue;
    private String categoryName;
    private String regionName;
}