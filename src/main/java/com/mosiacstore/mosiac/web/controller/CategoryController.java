package com.mosiacstore.mosiac.web.controller;

import com.mosiacstore.mosiac.application.dto.request.CategoryRequest;
import com.mosiacstore.mosiac.application.dto.response.ApiResponse;
import com.mosiacstore.mosiac.application.dto.response.CategoryResponse;
import com.mosiacstore.mosiac.application.dto.response.PageResponse;
import com.mosiacstore.mosiac.application.service.CategoryService;
import com.mosiacstore.mosiac.infrastructure.security.CustomUserDetail;
import com.mosiacstore.mosiac.infrastructure.service.MinioService;
import com.mosiacstore.mosiac.infrastructure.service.StorageServiceDelegate;
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
@Tag(name = "Categories", description = "Product Category API")
public class CategoryController {

    private final CategoryService categoryService;
    private final StorageServiceDelegate storageServiceDelegate;

    @Operation(summary = "Get all categories with pagination")
    @GetMapping("/categories")
    public ResponseEntity<PageResponse<CategoryResponse>> getCategories(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "displayOrder,asc") String sort) {

        return ResponseEntity.ok(categoryService.getCategories(page, size, sort));
    }

    @Operation(summary = "Get all categories without pagination")
    @GetMapping("/categories/all")
    public ResponseEntity<List<CategoryResponse>> getAllCategories() {
        return ResponseEntity.ok(categoryService.getAllCategories());
    }

    @Operation(summary = "Get category by ID")
    @GetMapping("/categories/{id}")
    public ResponseEntity<CategoryResponse> getCategoryById(@PathVariable UUID id) {
        return ResponseEntity.ok(categoryService.getCategoryById(id));
    }

    @Operation(
            summary = "Create a new category with image upload support",
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @PostMapping(
            value = "/admin/categories",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CategoryResponse> createCategory(
            @Valid @ModelAttribute CategoryRequest request,
            @AuthenticationPrincipal CustomUserDetail currentUser) {

        if (request.getFile() != null && !request.getFile().isEmpty()) {
            String url = storageServiceDelegate.uploadFile(request.getFile(), "categories");
            request.setImageUrl(url);
        }

        return new ResponseEntity<>(categoryService.createCategory(request), HttpStatus.CREATED);
    }

    @Operation(
            summary = "Update a category with image upload support",
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @PutMapping(
            value = "/admin/categories/{id}",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CategoryResponse> updateCategory(
            @PathVariable UUID id,
            @RequestParam String name,
            @RequestParam(required = false) String slug,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) UUID parentId,
            @RequestParam(defaultValue = "0") Integer displayOrder,
            @RequestParam(defaultValue = "true") Boolean active,
            @RequestParam(value = "file", required = false) MultipartFile file,
            @RequestParam(value = "imageUrl", required = false) String imageUrl,
            @AuthenticationPrincipal CustomUserDetail currentUser) {

        String newImageUrl = null;
        if (file != null && !file.isEmpty()) {
            newImageUrl = storageServiceDelegate.uploadFile(file, "categories");
        } else {
            newImageUrl = imageUrl;
        }

        CategoryRequest request = CategoryRequest.builder()
                .name(name)
                .slug(slug)
                .description(description)
                .parentId(parentId)
                .imageUrl(newImageUrl)
                .displayOrder(displayOrder)
                .active(active)
                .build();

        return ResponseEntity.ok(categoryService.updateCategory(id, request));
    }

    @Operation(
            summary = "Delete a category",
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @DeleteMapping("/admin/categories/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse> deleteCategory(
            @PathVariable UUID id,
            @AuthenticationPrincipal CustomUserDetail currentUser) {

        categoryService.deleteCategory(id);
        return ResponseEntity.ok(new ApiResponse(true, "Category deleted successfully"));
    }
}