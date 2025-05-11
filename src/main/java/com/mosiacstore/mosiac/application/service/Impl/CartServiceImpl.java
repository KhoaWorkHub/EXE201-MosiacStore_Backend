package com.mosiacstore.mosiac.application.service.Impl;

import com.mosiacstore.mosiac.application.dto.request.CartItemRequest;
import com.mosiacstore.mosiac.application.dto.response.CartResponse;
import com.mosiacstore.mosiac.application.exception.EntityNotFoundException;
import com.mosiacstore.mosiac.application.exception.InvalidOperationException;
import com.mosiacstore.mosiac.application.mapper.CartMapper;
import com.mosiacstore.mosiac.application.service.AdminNotificationService;
import com.mosiacstore.mosiac.application.service.CartService;
import com.mosiacstore.mosiac.domain.cart.Cart;
import com.mosiacstore.mosiac.domain.cart.CartItem;
import com.mosiacstore.mosiac.domain.product.Product;
import com.mosiacstore.mosiac.domain.product.ProductVariant;
import com.mosiacstore.mosiac.domain.user.User;
import com.mosiacstore.mosiac.infrastructure.repository.CartItemRepository;
import com.mosiacstore.mosiac.infrastructure.repository.CartRepository;
import com.mosiacstore.mosiac.infrastructure.repository.ProductRepository;
import com.mosiacstore.mosiac.infrastructure.repository.ProductVariantRepository;
import com.mosiacstore.mosiac.infrastructure.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class CartServiceImpl implements CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;
    private final ProductVariantRepository variantRepository;
    private final UserRepository userRepository;
    private final CartMapper cartMapper;
    private final AdminNotificationService adminNotificationService;


    private static final int CART_EXPIRATION_DAYS = 7;

    @Override
    @Transactional
    public CartResponse getOrCreateCart(UUID userId, String guestId) {
        Cart cart = findOrCreateCart(userId, guestId);
        return cartMapper.toCartResponse(cart);
    }

    @Override
    @Transactional
    public CartResponse addItemToCart(UUID userId, String guestId, CartItemRequest request) {
        if (request.getQuantity() <= 0) {
            throw new InvalidOperationException("Quantity must be greater than zero");
        }

        Cart cart = findOrCreateCart(userId, guestId);

        // Find product
        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new EntityNotFoundException("Product not found with ID: " + request.getProductId()));

        // Validate product
        if (!Boolean.TRUE.equals(product.getActive())) {
            throw new InvalidOperationException("Product is not active");
        }

        // Find variant if specified
        ProductVariant variant;
        if (request.getVariantId() != null) {
            variant = variantRepository.findById(request.getVariantId())
                    .orElseThrow(() -> new EntityNotFoundException("Product variant not found with ID: " + request.getVariantId()));

            // Validate variant
            if (!variant.getProduct().getId().equals(product.getId())) {
                throw new InvalidOperationException("Variant does not belong to the specified product");
            }

            if (!Boolean.TRUE.equals(variant.getActive())) {
                throw new InvalidOperationException("Product variant is not active");
            }
        } else {
            variant = null;
        }

        // Check stock
        Integer stock;
        if (variant != null) {
            stock = variant.getStockQuantity();
        } else {
            stock = product.getStockQuantity();
        }

        if (stock != null && stock < request.getQuantity()) {
            throw new InvalidOperationException("Not enough stock available");
        }

        // Check if item already exists in cart
        Optional<CartItem> existingItem = cart.getItems().stream()
                .filter(item -> item.getProduct().getId().equals(product.getId()))
                .filter(item -> {
                    if (variant == null && item.getVariant() == null) {
                        return true;
                    } else if (variant != null && item.getVariant() != null) {
                        return item.getVariant().getId().equals(variant.getId());
                    }
                    return false;
                })
                .findFirst();

        String activityMessage;

        if (existingItem.isPresent()) {
            // Update quantity of existing item
            CartItem item = existingItem.get();
            int oldQuantity = item.getQuantity();
            int newQuantity = item.getQuantity() + request.getQuantity();

            // Check stock again for combined quantity
            if (stock != null && stock < newQuantity) {
                throw new InvalidOperationException("Not enough stock available");
            }

            item.setQuantity(newQuantity);
            cartItemRepository.save(item);

            activityMessage = "updated quantity of " + product.getName() + " from " + oldQuantity + " to " + newQuantity;
        } else {
            // Create new cart item
            CartItem newItem = new CartItem();
            newItem.setCart(cart);
            newItem.setProduct(product);
            newItem.setVariant(variant);
            newItem.setQuantity(request.getQuantity());

            // Calculate price
            BigDecimal price = product.getPrice();
            if (variant != null && variant.getPriceAdjustment() != null) {
                price = price.add(variant.getPriceAdjustment());
            }
            newItem.setPriceSnapshot(price);

            newItem.setAddedAt(LocalDateTime.now());
            cart.getItems().add(newItem);
            cartItemRepository.save(newItem);

            activityMessage = "added " + request.getQuantity() + " of " + product.getName() + " to cart";
        }

        // Update cart expiration
        cart.setExpiredAt(LocalDateTime.now().plusDays(CART_EXPIRATION_DAYS));
        cart.setUpdatedAt(LocalDateTime.now());
        cartRepository.save(cart);

        // Send admin notification asynchronously
        adminNotificationService.notifyCartActivity(cart, activityMessage, cart.getItems().size());

        return cartMapper.toCartResponse(cart);
    }

    @Override
    @Transactional
    public CartResponse updateCartItemQuantity(UUID userId, String guestId, UUID itemId, int quantity) {
        if (quantity <= 0) {
            throw new InvalidOperationException("Quantity must be greater than zero");
        }

        Cart cart = findCart(userId, guestId);

        CartItem item = cartItemRepository.findById(itemId)
                .orElseThrow(() -> new EntityNotFoundException("Cart item not found with ID: " + itemId));

        // Verify item belongs to the cart
        if (!item.getCart().getId().equals(cart.getId())) {
            throw new InvalidOperationException("Item does not belong to the current cart");
        }

        // Check stock
        Integer stock;
        if (item.getVariant() != null) {
            stock = item.getVariant().getStockQuantity();
        } else {
            stock = item.getProduct().getStockQuantity();
        }

        if (stock != null && stock < quantity) {
            throw new InvalidOperationException("Not enough stock available");
        }

        item.setQuantity(quantity);
        cartItemRepository.save(item);

        // Update cart expiration
        cart.setExpiredAt(LocalDateTime.now().plusDays(CART_EXPIRATION_DAYS));
        cart.setUpdatedAt(LocalDateTime.now());
        cartRepository.save(cart);

        return cartMapper.toCartResponse(cart);
    }

    @Override
    @Transactional
    public CartResponse removeCartItem(UUID userId, String guestId, UUID itemId) {
        Cart cart = findCart(userId, guestId);

        CartItem item = cartItemRepository.findById(itemId)
                .orElseThrow(() -> new EntityNotFoundException("Cart item not found with ID: " + itemId));

        // Verify item belongs to the cart
        if (!item.getCart().getId().equals(cart.getId())) {
            throw new InvalidOperationException("Item does not belong to the current cart");
        }

        cart.getItems().remove(item);
        cartItemRepository.delete(item);

        // Update cart expiration and timestamp
        cart.setExpiredAt(LocalDateTime.now().plusDays(CART_EXPIRATION_DAYS));
        cart.setUpdatedAt(LocalDateTime.now());
        cartRepository.save(cart);

        return cartMapper.toCartResponse(cart);
    }

    @Override
    @Transactional
    public void clearCart(UUID userId, String guestId) {
        Cart cart = findCart(userId, guestId);

        cart.getItems().clear();

        // Update cart
        cart.setExpiredAt(LocalDateTime.now().plusDays(CART_EXPIRATION_DAYS));
        cart.setUpdatedAt(LocalDateTime.now());
        cartRepository.save(cart);
    }

    @Override
    @Transactional
    public CartResponse mergeGuestCartWithUserCart(UUID userId, String guestId) {
        if (userId == null) {
            throw new InvalidOperationException("User ID is required for merging carts");
        }

        if (guestId == null || guestId.trim().isEmpty()) {
            throw new InvalidOperationException("Guest ID is required for merging carts");
        }

        // Find user cart or create if not exists
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found with ID: " + userId));

        Cart userCart = cartRepository.findByUserId(userId)
                .orElseGet(() -> {
                    Cart newCart = new Cart();
                    newCart.setUser(user);
                    newCart.setExpiredAt(LocalDateTime.now().plusDays(CART_EXPIRATION_DAYS));
                    newCart.setItems(new HashSet<>());
                    return cartRepository.save(newCart);
                });

        // Find guest cart
        Cart guestCart = cartRepository.findByGuestId(guestId)
                .orElse(null);

        // No guest cart, return user cart
        if (guestCart == null || guestCart.getItems().isEmpty()) {
            return cartMapper.toCartResponse(userCart);
        }

        // Merge items from guest cart to user cart
        for (CartItem guestItem : guestCart.getItems()) {
            // Check if product already in user cart
            Optional<CartItem> existingItem = userCart.getItems().stream()
                    .filter(item -> item.getProduct().getId().equals(guestItem.getProduct().getId()))
                    .filter(item -> {
                        if (guestItem.getVariant() == null && item.getVariant() == null) {
                            return true;
                        } else if (guestItem.getVariant() != null && item.getVariant() != null) {
                            return item.getVariant().getId().equals(guestItem.getVariant().getId());
                        }
                        return false;
                    })
                    .findFirst();

            if (existingItem.isPresent()) {
                // Update quantity
                CartItem userItem = existingItem.get();
                userItem.setQuantity(userItem.getQuantity() + guestItem.getQuantity());
                cartItemRepository.save(userItem);
            } else {
                // Create new item in user cart
                CartItem newItem = new CartItem();
                newItem.setCart(userCart);
                newItem.setProduct(guestItem.getProduct());
                newItem.setVariant(guestItem.getVariant());
                newItem.setQuantity(guestItem.getQuantity());
                newItem.setPriceSnapshot(guestItem.getPriceSnapshot());
                newItem.setAddedAt(LocalDateTime.now());

                userCart.getItems().add(newItem);
                cartItemRepository.save(newItem);
            }
        }

        // Delete guest cart
        cartRepository.delete(guestCart);

        // Update user cart
        userCart.setExpiredAt(LocalDateTime.now().plusDays(CART_EXPIRATION_DAYS));
        userCart.setUpdatedAt(LocalDateTime.now());
        cartRepository.save(userCart);

        return cartMapper.toCartResponse(userCart);
    }

    // Helper methods

    private Cart findOrCreateCart(UUID userId, String guestId) {
        // Try to find existing cart
        Cart cart = findCartOrNull(userId, guestId);

        // Create new cart if not found
        if (cart == null) {
            cart = new Cart();

            if (userId != null) {
                User user = userRepository.findById(userId)
                        .orElseThrow(() -> new EntityNotFoundException("User not found with ID: " + userId));
                cart.setUser(user);
            } else {
                cart.setGuestId(guestId);
            }

            cart.setExpiredAt(LocalDateTime.now().plusDays(CART_EXPIRATION_DAYS));
            cart.setItems(new HashSet<>());
            cart = cartRepository.save(cart);
        }

        return cart;
    }

    private Cart findCart(UUID userId, String guestId) {
        Cart cart = findCartOrNull(userId, guestId);

        if (cart == null) {
            if (userId != null) {
                throw new EntityNotFoundException("Cart not found for user with ID: " + userId);
            } else {
                throw new EntityNotFoundException("Cart not found for guest with ID: " + guestId);
            }
        }

        return cart;
    }

    private Cart findCartOrNull(UUID userId, String guestId) {
        // Validate input
        if (userId == null && (guestId == null || guestId.trim().isEmpty())) {
            throw new InvalidOperationException("Either user ID or guest ID must be provided");
        }

        // Try to find by user ID
        if (userId != null) {
            return cartRepository.findByUserId(userId).orElse(null);
        }

        // Try to find by guest ID
        return cartRepository.findByGuestId(guestId).orElse(null);
    }
}