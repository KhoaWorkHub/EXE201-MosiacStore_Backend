package com.mosiacstore.mosiac.web.controller;

import com.mosiacstore.mosiac.application.dto.request.RegionRequest;
import com.mosiacstore.mosiac.application.dto.response.ApiResponse;
import com.mosiacstore.mosiac.application.dto.response.PageResponse;
import com.mosiacstore.mosiac.application.dto.response.RegionResponse;
import com.mosiacstore.mosiac.application.service.RegionService;
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
@Tag(name = "Regions", description = "Region API")
public class RegionController {

    private final RegionService regionService;
    private final StorageServiceDelegate storageServiceDelegate;

    @Operation(summary = "Get all regions with pagination")
    @GetMapping("/regions")
    public ResponseEntity<PageResponse<RegionResponse>> getRegions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "name,asc") String sort) {

        return ResponseEntity.ok(regionService.getRegions(page, size, sort));
    }

    @Operation(summary = "Get all regions without pagination")
    @GetMapping("/regions/all")
    public ResponseEntity<List<RegionResponse>> getAllRegions() {
        return ResponseEntity.ok(regionService.getAllRegions());
    }

    @Operation(summary = "Get region by ID")
    @GetMapping("/regions/{id}")
    public ResponseEntity<RegionResponse> getRegionById(@PathVariable UUID id) {
        return ResponseEntity.ok(regionService.getRegionById(id));
    }

    @PostMapping(
            value = "/admin/regions",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<RegionResponse> createRegion(
            @Valid @ModelAttribute RegionRequest request,
            @AuthenticationPrincipal CustomUserDetail currentUser
    ) {
        if (request.getFile() != null && !request.getFile().isEmpty()) {
            String url = storageServiceDelegate.uploadFile(request.getFile(), "regions");
            request.setImageUrl(url);
        }
        RegionResponse created = regionService.createRegion(request);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }


    @PutMapping(
            value = "/admin/regions/{id}",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<RegionResponse> updateRegion(
            @PathVariable UUID id,
            @RequestParam String name,
            @RequestParam String slug,
            @RequestParam(required = false) String description,
            @RequestParam(defaultValue = "true") boolean active,
            @RequestParam(value = "file", required = false) MultipartFile file,
            @RequestParam(value = "imageUrl", required = false) String imageUrl,
            @AuthenticationPrincipal CustomUserDetail currentUser
    ) {
        String newImageUrl = null;
        if (file != null && !file.isEmpty()) {
            newImageUrl = storageServiceDelegate.uploadFile(file, "regions");
        } else {
            newImageUrl = imageUrl;
        }

        RegionRequest dto = RegionRequest.builder()
                .name(name)
                .slug(slug)
                .description(description)
                .active(active)
                .imageUrl(newImageUrl)
                .build();

        RegionResponse updated = regionService.updateRegion(id, dto);
        return ResponseEntity.ok(updated);
    }


    @Operation(
            summary = "Delete a region",
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @DeleteMapping("/admin/regions/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse> deleteRegion(
            @PathVariable UUID id,
            @AuthenticationPrincipal CustomUserDetail currentUser) {

        regionService.deleteRegion(id);
        return ResponseEntity.ok(new ApiResponse(true, "Region deleted successfully"));
    }
}