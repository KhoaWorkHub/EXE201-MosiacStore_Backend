package com.mosiacstore.mosiac.application.service;

import com.mosiacstore.mosiac.application.dto.request.ProductRequest;
import com.mosiacstore.mosiac.application.dto.request.ProductVariantRequest;
import com.mosiacstore.mosiac.application.dto.response.PageResponse;
import com.mosiacstore.mosiac.application.dto.response.ProductImageResponse;
import com.mosiacstore.mosiac.application.dto.response.ProductResponse;
import com.mosiacstore.mosiac.application.dto.response.ProductVariantResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

public interface ProductService {

    PageResponse<ProductResponse> getProducts(
            String keyword,
            UUID categoryId,
            UUID regionId,
            Double minPrice,
            Double maxPrice,
            Boolean featured,
            Boolean active,
            int page,
            int size,
            String sort
    );

    ProductResponse getProductById(UUID id);

    ProductResponse getProductBySlug(String slug);

    ProductResponse createProduct(ProductRequest request);

    ProductResponse updateProduct(UUID id, ProductRequest request);

    void deleteProduct(UUID id);

    List<ProductImageResponse> uploadProductImages(
            UUID productId,
            List<MultipartFile> files,
            String altText,
            Boolean isPrimary
    );

    void deleteProductImage(UUID imageId);

    ProductResponse setProductFeatured(UUID id, boolean featured);

    ProductVariantResponse addProductVariant(
            UUID productId,
            ProductVariantRequest request
    );

    ProductVariantResponse updateProductVariant(
            UUID variantId,
            ProductVariantRequest request
    );

    void deleteProductVariant(UUID variantId);

    PageResponse<ProductResponse> getFeaturedProducts(int page, int size);
}
