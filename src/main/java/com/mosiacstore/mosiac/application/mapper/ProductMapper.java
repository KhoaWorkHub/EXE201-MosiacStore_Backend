package com.mosiacstore.mosiac.application.mapper;

import com.mosiacstore.mosiac.application.dto.request.ProductRequest;
import com.mosiacstore.mosiac.application.dto.request.ProductVariantRequest;
import com.mosiacstore.mosiac.application.dto.response.ProductImageResponse;
import com.mosiacstore.mosiac.application.dto.response.ProductResponse;
import com.mosiacstore.mosiac.application.dto.response.ProductVariantResponse;
import com.mosiacstore.mosiac.domain.product.Product;
import com.mosiacstore.mosiac.domain.product.ProductCategory;
import com.mosiacstore.mosiac.domain.product.ProductImage;
import com.mosiacstore.mosiac.domain.product.ProductVariant;
import com.mosiacstore.mosiac.domain.qrcode.QRCode;
import com.mosiacstore.mosiac.domain.region.Region;
import org.mapstruct.*;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ProductMapper {

    // Product mappings
    @Mapping(target = "category", source = "category", qualifiedByName = "mapCategory")
    @Mapping(target = "region", source = "region", qualifiedByName = "mapRegion")
    @Mapping(target = "qrCode", source = "qrCode", qualifiedByName = "mapQRCode")
    @Mapping(target = "variants", source = "variants")
    @Mapping(target = "images", source = "images")
    ProductResponse toProductResponse(Product product);

    List<ProductResponse> toProductResponseList(List<Product> products);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "viewCount", ignore = true)
    @Mapping(target = "variants", ignore = true)
    @Mapping(target = "images", ignore = true)
    @Mapping(target = "qrCode", ignore = true)
    @Mapping(target = "category", ignore = true)
    @Mapping(target = "region", ignore = true)
    Product toProduct(ProductRequest productRequest);

    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateProductFromRequest(ProductRequest request, @MappingTarget Product product);

    // Variant mappings
    ProductVariantResponse toProductVariantResponse(ProductVariant variant);

    List<ProductVariantResponse> toProductVariantResponseList(List<ProductVariant> variants);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "product", ignore = true)
    ProductVariant toProductVariant(ProductVariantRequest request);

    // Image mappings
    ProductImageResponse toProductImageResponse(ProductImage image);

    List<ProductImageResponse> toProductImageResponseList(List<ProductImage> images);

    // Helper methods for nested mappings
    @Named("mapCategory")
    default ProductResponse.CategoryResponse mapCategory(ProductCategory category) {
        if (category == null) {
            return null;
        }
        return ProductResponse.CategoryResponse.builder()
                .id(category.getId())
                .name(category.getName())
                .slug(category.getSlug())
                .build();
    }

    @Named("mapRegion")
    default ProductResponse.RegionResponse mapRegion(Region region) {
        if (region == null) {
            return null;
        }
        return ProductResponse.RegionResponse.builder()
                .id(region.getId())
                .name(region.getName())
                .slug(region.getSlug())
                .build();
    }

    @Named("mapQRCode")
    default ProductResponse.QRCodeResponse mapQRCode(QRCode qrCode) {
        if (qrCode == null) {
            return null;
        }
        return ProductResponse.QRCodeResponse.builder()
                .id(qrCode.getId())
                .qrImageUrl(qrCode.getQrImageUrl())
                .qrData(qrCode.getQrData())
                .redirectUrl(qrCode.getRedirectUrl())
                .build();
    }
}