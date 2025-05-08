package com.mosiacstore.mosiac.web.controller;

import com.mosiacstore.mosiac.application.dto.request.AddressRequest;
import com.mosiacstore.mosiac.application.dto.response.AddressResponse;
import com.mosiacstore.mosiac.application.dto.response.ApiResponse;
import com.mosiacstore.mosiac.application.dto.response.DistrictResponse;
import com.mosiacstore.mosiac.application.dto.response.ProvinceResponse;
import com.mosiacstore.mosiac.application.dto.response.WardResponse;
import com.mosiacstore.mosiac.application.service.AddressService;
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
@Tag(name = "Addresses", description = "Address API")
public class AddressController {

    private final AddressService addressService;

    @Operation(summary = "Get all provinces of Vietnam")
    @GetMapping("/provinces")
    public ResponseEntity<List<ProvinceResponse>> getAllProvinces() {
        return ResponseEntity.ok(addressService.getAllProvinces());
    }

    @Operation(summary = "Get districts by province code")
    @GetMapping("/provinces/{provinceCode}/districts")
    public ResponseEntity<List<DistrictResponse>> getDistrictsByProvince(
            @PathVariable String provinceCode) {
        return ResponseEntity.ok(addressService.getDistrictsByProvince(provinceCode));
    }

    @Operation(summary = "Get wards by district code")
    @GetMapping("/districts/{districtCode}/wards")
    public ResponseEntity<List<WardResponse>> getWardsByDistrict(
            @PathVariable String districtCode) {
        return ResponseEntity.ok(addressService.getWardsByDistrict(districtCode));
    }

    @Operation(
            summary = "Get user addresses",
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @GetMapping("/user/addresses")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<AddressResponse>> getUserAddresses(
            @AuthenticationPrincipal CustomUserDetail currentUser) {
        return ResponseEntity.ok(addressService.getUserAddresses(currentUser.getUser().getId()));
    }

    @Operation(
            summary = "Get address by ID",
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @GetMapping("/user/addresses/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<AddressResponse> getAddressById(
            @PathVariable UUID id,
            @AuthenticationPrincipal CustomUserDetail currentUser) {
        return ResponseEntity.ok(addressService.getAddressById(id, currentUser.getUser().getId()));
    }

    @Operation(
            summary = "Create address",
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @PostMapping("/user/addresses")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<AddressResponse> createAddress(
            @Valid @RequestBody AddressRequest request,
            @AuthenticationPrincipal CustomUserDetail currentUser) {
        AddressResponse response = addressService.createAddress(request, currentUser.getUser().getId());
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @Operation(
            summary = "Update address",
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @PutMapping("/user/addresses/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<AddressResponse> updateAddress(
            @PathVariable UUID id,
            @Valid @RequestBody AddressRequest request,
            @AuthenticationPrincipal CustomUserDetail currentUser) {
        return ResponseEntity.ok(addressService.updateAddress(id, request, currentUser.getUser().getId()));
    }

    @Operation(
            summary = "Delete address",
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @DeleteMapping("/user/addresses/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse> deleteAddress(
            @PathVariable UUID id,
            @AuthenticationPrincipal CustomUserDetail currentUser) {
        addressService.deleteAddress(id, currentUser.getUser().getId());
        return ResponseEntity.ok(new ApiResponse(true, "Address deleted successfully"));
    }

    @Operation(
            summary = "Set default address",
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @PutMapping("/user/addresses/{id}/default")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<AddressResponse> setDefaultAddress(
            @PathVariable UUID id,
            @AuthenticationPrincipal CustomUserDetail currentUser) {
        return ResponseEntity.ok(addressService.setDefaultAddress(id, currentUser.getUser().getId()));
    }
}