package com.mosiacstore.mosiac.application.dto.request;

import com.mosiacstore.mosiac.domain.product.ProductSize;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductVariantRequest {
    @NotNull(message = "Size is required")
    private String size;

    private String color;

    private BigDecimal priceAdjustment;

    private Integer stockQuantity;

    private String skuVariant;

    private Boolean active;
}