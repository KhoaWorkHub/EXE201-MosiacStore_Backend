package com.mosiacstore.mosiac.application.mapper;

import com.mosiacstore.mosiac.application.dto.response.CartItemResponse;
import com.mosiacstore.mosiac.application.dto.response.CartResponse;
import com.mosiacstore.mosiac.domain.cart.Cart;
import com.mosiacstore.mosiac.domain.cart.CartItem;
import com.mosiacstore.mosiac.domain.product.ProductImage;
import org.mapstruct.*;

import java.math.BigDecimal;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE, imports = {BigDecimal.class} )
public interface CartMapper {

    @Mapping(target = "totalAmount", ignore = true)
    @Mapping(target = "totalItems", ignore = true)
    @Mapping(target = "items", source = "items")
    CartResponse toCartResponse(Cart cart);

    @AfterMapping
    default void calculateTotals(Cart cart, @MappingTarget CartResponse response) {
        BigDecimal total = BigDecimal.ZERO;
        int itemCount = 0;

        for (CartItem item : cart.getItems()) {
            total = total.add(item.getPriceSnapshot().multiply(new BigDecimal(item.getQuantity())));
            itemCount += item.getQuantity();
        }

        response.setTotalAmount(total);
        response.setTotalItems(itemCount);
    }

    @Mapping(target = "productName", source = "product.name")
    @Mapping(target = "productId", source = "product.id")
    @Mapping(target = "productSlug", source = "product.slug")
    @Mapping(target = "productImage", expression = "java(getProductImageUrl(cartItem))")
    @Mapping(target = "variantInfo", expression = "java(getVariantInfo(cartItem))")
    @Mapping(target = "price", source = "priceSnapshot")
    @Mapping(target = "subtotal", expression = "java(cartItem.getPriceSnapshot().multiply(new BigDecimal(cartItem.getQuantity())))")
    CartItemResponse toCartItemResponse(CartItem cartItem);

    default String getProductImageUrl(CartItem cartItem) {
        return cartItem.getProduct().getImages().stream()
                .filter(ProductImage::getIsPrimary)
                .findFirst()
                .map(ProductImage::getImageUrl)
                .orElse(null);
    }

    default String getVariantInfo(CartItem cartItem) {
        if (cartItem.getVariant() == null) {
            return null;
        }
        StringBuilder info = new StringBuilder();
        if (cartItem.getVariant().getSize() != null) {
            info.append("Size: ").append(cartItem.getVariant().getSize().name());
        }
        if (cartItem.getVariant().getColor() != null) {
            if (info.length() > 0) info.append(", ");
            info.append("Color: ").append(cartItem.getVariant().getColor());
        }
        return info.toString();
    }
}