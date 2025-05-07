package com.mosiacstore.mosiac.web.controller;

import com.mosiacstore.mosiac.application.dto.request.CartItemRequest;
import com.mosiacstore.mosiac.application.dto.response.ApiResponse;
import com.mosiacstore.mosiac.application.dto.response.CartResponse;
import com.mosiacstore.mosiac.application.service.CartService;
import com.mosiacstore.mosiac.infrastructure.security.CustomUserDetail;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/cart")
@RequiredArgsConstructor
@Tag(name = "Cart", description = "Shopping Cart API")
public class CartController {

    private final CartService cartService;

    @Operation(summary = "Get current cart")
    @GetMapping
    public ResponseEntity<CartResponse> getCart(
            @AuthenticationPrincipal CustomUserDetail currentUser,
            @RequestParam(required = false) String guestId) {

        UUID userId = currentUser != null ? currentUser.getUser().getId() : null;
        return ResponseEntity.ok(cartService.getOrCreateCart(userId, guestId));
    }

    @Operation(summary = "Add item to cart")
    @PostMapping("/items")
    public ResponseEntity<CartResponse> addItemToCart(
            @AuthenticationPrincipal CustomUserDetail currentUser,
            @RequestParam(required = false) String guestId,
            @Valid @RequestBody CartItemRequest request) {

        UUID userId = currentUser != null ? currentUser.getUser().getId() : null;
        return ResponseEntity.ok(cartService.addItemToCart(userId, guestId, request));
    }

    @Operation(summary = "Update cart item quantity")
    @PutMapping("/items/{itemId}")
    public ResponseEntity<CartResponse> updateCartItem(
            @AuthenticationPrincipal CustomUserDetail currentUser,
            @RequestParam(required = false) String guestId,
            @PathVariable UUID itemId,
            @RequestParam int quantity) {

        UUID userId = currentUser != null ? currentUser.getUser().getId() : null;
        return ResponseEntity.ok(cartService.updateCartItemQuantity(userId, guestId, itemId, quantity));
    }

    @Operation(summary = "Remove item from cart")
    @DeleteMapping("/items/{itemId}")
    public ResponseEntity<CartResponse> removeCartItem(
            @AuthenticationPrincipal CustomUserDetail currentUser,
            @RequestParam(required = false) String guestId,
            @PathVariable UUID itemId) {

        UUID userId = currentUser != null ? currentUser.getUser().getId() : null;
        return ResponseEntity.ok(cartService.removeCartItem(userId, guestId, itemId));
    }

    @Operation(summary = "Clear cart (remove all items)")
    @DeleteMapping
    public ResponseEntity<ApiResponse> clearCart(
            @AuthenticationPrincipal CustomUserDetail currentUser,
            @RequestParam(required = false) String guestId) {

        UUID userId = currentUser != null ? currentUser.getUser().getId() : null;
        cartService.clearCart(userId, guestId);
        return ResponseEntity.ok(new ApiResponse(true, "Cart cleared successfully"));
    }

    @Operation(summary = "Merge guest cart with user cart after login")
    @PostMapping("/merge")
    public ResponseEntity<CartResponse> mergeGuestCart(
            @AuthenticationPrincipal CustomUserDetail currentUser,
            @RequestParam String guestId) {

        if (currentUser == null) {
            return ResponseEntity.badRequest().build();
        }

        UUID userId = currentUser.getUser().getId();
        return ResponseEntity.ok(cartService.mergeGuestCartWithUserCart(userId, guestId));
    }
}