package com.mosiacstore.mosiac.application.service;

import com.mosiacstore.mosiac.application.dto.request.CartItemRequest;
import com.mosiacstore.mosiac.application.dto.response.CartResponse;

import java.util.UUID;

public interface CartService {
    /**
     * Get or create cart for a user or guest
     * @param userId Optional user ID for authenticated users
     * @param guestId Optional guest ID for non-authenticated users
     * @return Cart response with items
     */
    CartResponse getOrCreateCart(UUID userId, String guestId);

    /**
     * Add item to cart
     * @param userId Optional user ID
     * @param guestId Optional guest ID
     * @param request Cart item request
     * @return Updated cart
     */
    CartResponse addItemToCart(UUID userId, String guestId, CartItemRequest request);

    /**
     * Update cart item quantity
     * @param userId Optional user ID
     * @param guestId Optional guest ID
     * @param itemId Cart item ID
     * @param quantity New quantity
     * @return Updated cart
     */
    CartResponse updateCartItemQuantity(UUID userId, String guestId, UUID itemId, int quantity);

    /**
     * Remove item from cart
     * @param userId Optional user ID
     * @param guestId Optional guest ID
     * @param itemId Cart item ID
     * @return Updated cart
     */
    CartResponse removeCartItem(UUID userId, String guestId, UUID itemId);

    /**
     * Clear cart (remove all items)
     * @param userId Optional user ID
     * @param guestId Optional guest ID
     */
    void clearCart(UUID userId, String guestId);

    /**
     * Merge guest cart with user cart after login
     * @param userId User ID
     * @param guestId Guest ID
     * @return Merged cart
     */
    CartResponse mergeGuestCartWithUserCart(UUID userId, String guestId);
}