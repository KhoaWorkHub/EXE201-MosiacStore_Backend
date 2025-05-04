package com.mosiacstore.mosiac.web.controller;

import com.mosiacstore.mosiac.application.dto.request.ProductRequest;
import com.mosiacstore.mosiac.application.dto.request.ProductVariantRequest;
import com.mosiacstore.mosiac.application.dto.response.ApiResponse;
import com.mosiacstore.mosiac.application.dto.response.PageResponse;
import com.mosiacstore.mosiac.application.dto.response.ProductImageResponse;
import com.mosiacstore.mosiac.application.dto.response.ProductResponse;
import com.mosiacstore.mosiac.application.dto.response.ProductVariantResponse;
import com.mosiacstore.mosiac.application.service.ProductService;
import com.mosiacstore.mosiac.infrastructure.security.CustomUserDetail;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "Products", description = "Product API")
public class ProductController {

    private final ProductService productService;

    @Operation(summary = "Get all products with pagination and filtering")
    @GetMapping("/products")
    public ResponseEntity<PageResponse<ProductResponse>> getProducts(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) UUID categoryId,
            @RequestParam(required = false) UUID regionId,
            @RequestParam(required = false) Double minPrice,
            @RequestParam(required = false) Double maxPrice,
            @RequestParam(required = false) Boolean featured,
            @RequestParam(required = false) Boolean active,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt,desc") String sort) {

        return ResponseEntity.ok(productService.getProducts(
                keyword, categoryId, regionId, minPrice, maxPrice, featured, active, page, size, sort));
    }

    @Operation(summary = "Get product by ID")
    @GetMapping("/products/{id}")
    public ResponseEntity<ProductResponse> getProductById(@PathVariable UUID id) {
        return ResponseEntity.ok(productService.getProductById(id));
    }

    @Operation(summary = "Get product by slug")
    @GetMapping("/products/slug/{slug}")
    public ResponseEntity<ProductResponse> getProductBySlug(@PathVariable String slug) {
        return ResponseEntity.ok(productService.getProductBySlug(slug));
    }

    @Operation(summary = "Get featured products")
    @GetMapping("/products/featured")
    public ResponseEntity<PageResponse<ProductResponse>> getFeaturedProducts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        return ResponseEntity.ok(productService.getFeaturedProducts(page, size));
    }

    @Operation(
            summary = "Create a new product",
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @PostMapping("/admin/products")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ProductResponse> createProduct(
            @Valid @RequestBody ProductRequest request,
            @AuthenticationPrincipal CustomUserDetail currentUser) {

        return new ResponseEntity<>(productService.createProduct(request), HttpStatus.CREATED);
    }

    @Operation(
            summary = "Update a product",
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @PutMapping("/admin/products/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ProductResponse> updateProduct(
            @PathVariable UUID id,
            @Valid @RequestBody ProductRequest request,
            @AuthenticationPrincipal CustomUserDetail currentUser) {

        return ResponseEntity.ok(productService.updateProduct(id, request));
    }

    @Operation(
            summary = "Delete a product",
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @DeleteMapping("/admin/products/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse> deleteProduct(
            @PathVariable UUID id,
            @AuthenticationPrincipal CustomUserDetail currentUser) {

        productService.deleteProduct(id);
        return ResponseEntity.ok(new ApiResponse(true, "Product deleted successfully"));
    }

    @Operation(
            summary = "Upload images for a product",
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @PostMapping(value = "/admin/products/{id}/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<ProductImageResponse>> uploadProductImages(
            @PathVariable UUID id,
            @RequestParam("files") List<MultipartFile> files,
            @RequestParam(required = false) String altText,
            @RequestParam(required = false, defaultValue = "false") Boolean isPrimary,
            @AuthenticationPrincipal CustomUserDetail currentUser) {

        return ResponseEntity.ok(productService.uploadProductImages(id, files, altText, isPrimary));
    }

    @Operation(
            summary = "Delete a product image",
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @DeleteMapping("/admin/products/images/{imageId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse> deleteProductImage(
            @PathVariable UUID imageId,
            @AuthenticationPrincipal CustomUserDetail currentUser) {

        productService.deleteProductImage(imageId);
        return ResponseEntity.ok(new ApiResponse(true, "Product image deleted successfully"));
    }

    @Operation(
            summary = "Set product featured status",
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @PutMapping("/admin/products/{id}/featured")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ProductResponse> setProductFeatured(
            @PathVariable UUID id,
            @RequestParam boolean featured,
            @AuthenticationPrincipal CustomUserDetail currentUser) {

        return ResponseEntity.ok(productService.setProductFeatured(id, featured));
    }

    @Operation(
            summary = "Add variant to product",
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @PostMapping("/admin/products/{id}/variants")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ProductVariantResponse> addProductVariant(
            @PathVariable UUID id,
            @Valid @RequestBody ProductVariantRequest request,
            @AuthenticationPrincipal CustomUserDetail currentUser) {

        return new ResponseEntity<>(productService.addProductVariant(id, request), HttpStatus.CREATED);
    }

    @Operation(
            summary = "Update product variant",
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @PutMapping("/admin/products/variants/{variantId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ProductVariantResponse> updateProductVariant(
            @PathVariable UUID variantId,
            @Valid @RequestBody ProductVariantRequest request,
            @AuthenticationPrincipal CustomUserDetail currentUser) {

        return ResponseEntity.ok(productService.updateProductVariant(variantId, request));
    }

    @Operation(
            summary = "Delete product variant",
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @DeleteMapping("/admin/products/variants/{variantId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse> deleteProductVariant(
            @PathVariable UUID variantId,
            @AuthenticationPrincipal CustomUserDetail currentUser) {

        productService.deleteProductVariant(variantId);
        return ResponseEntity.ok(new ApiResponse(true, "Product variant deleted successfully"));
    }
}