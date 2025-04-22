package com.mosiacstore.mosiac.web.controller;

import com.mosiacstore.mosiac.application.dto.request.RegionRequest;
import com.mosiacstore.mosiac.application.dto.response.ApiResponse;
import com.mosiacstore.mosiac.application.dto.response.PageResponse;
import com.mosiacstore.mosiac.application.dto.response.RegionResponse;
import com.mosiacstore.mosiac.application.service.RegionService;
import com.mosiacstore.mosiac.infrastructure.security.CustomUserDetail;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "Regions", description = "Region API")
public class RegionController {

    private final RegionService regionService;

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

    @Operation(
            summary = "Create a new region",
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @PostMapping("/admin/regions")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<RegionResponse> createRegion(
            @Valid @RequestBody RegionRequest request,
            @AuthenticationPrincipal CustomUserDetail currentUser) {

        return new ResponseEntity<>(regionService.createRegion(request), HttpStatus.CREATED);
    }

    @Operation(
            summary = "Update a region",
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @PutMapping("/admin/regions/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<RegionResponse> updateRegion(
            @PathVariable UUID id,
            @Valid @RequestBody RegionRequest request,
            @AuthenticationPrincipal CustomUserDetail currentUser) {

        return ResponseEntity.ok(regionService.updateRegion(id, request));
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