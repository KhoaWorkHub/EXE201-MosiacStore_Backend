package com.mosiacstore.mosiac.web.controller;

import com.mosiacstore.mosiac.application.dto.UserDto;
import com.mosiacstore.mosiac.application.dto.request.UserProfileRequest;
import com.mosiacstore.mosiac.application.service.UserService;
import com.mosiacstore.mosiac.infrastructure.security.CustomUserDetail;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/user/profile")
@RequiredArgsConstructor
@Tag(name = "User Profile", description = "User Profile API")
public class UserController {

    private final UserService userService;

    @Operation(
            summary = "Get user profile",
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UserDto> getUserProfile(
            @AuthenticationPrincipal CustomUserDetail currentUser) {
        return ResponseEntity.ok(userService.getUserProfile(currentUser.getUser().getId()));
    }

    @Operation(
            summary = "Update user profile",
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @PutMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UserDto> updateUserProfile(
            @Valid @RequestBody UserProfileRequest request,
            @AuthenticationPrincipal CustomUserDetail currentUser) {
        return ResponseEntity.ok(userService.updateUserProfile(currentUser.getUser().getId(), request));
    }
}