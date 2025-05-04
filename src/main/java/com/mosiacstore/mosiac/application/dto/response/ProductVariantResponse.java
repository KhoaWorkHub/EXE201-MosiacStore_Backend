package com.mosiacstore.mosiac.application.dto.response;

import com.mosiacstore.mosiac.domain.product.ProductSize;
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
public class ProductVariantResponse {
    private UUID id;
    private String size;
    private String color;
    private BigDecimal priceAdjustment;
    private Integer stockQuantity;
    private String skuVariant;
    private Boolean active;
}