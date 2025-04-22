package com.mosiacstore.mosiac.application.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductResponse {
    private UUID id;
    private String name;
    private String slug;
    private String description;
    private String shortDescription;
    private BigDecimal price;
    private BigDecimal originalPrice;
    private Integer stockQuantity;
    private String sku;
    private Boolean active;
    private Boolean featured;
    private Integer viewCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private CategoryResponse category;
    private RegionResponse region;
    private List<ProductVariantResponse> variants;
    private List<ProductImageResponse> images;
    private QRCodeResponse qrCode;

    // Nested response classes
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CategoryResponse {
        private UUID id;
        private String name;
        private String slug;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RegionResponse {
        private UUID id;
        private String name;
        private String slug;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class QRCodeResponse {
        private UUID id;
        private String qrImageUrl;
        private String qrData;
        private String redirectUrl;
    }
}