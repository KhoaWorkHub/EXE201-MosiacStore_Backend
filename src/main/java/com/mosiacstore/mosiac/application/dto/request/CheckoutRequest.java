package com.mosiacstore.mosiac.application.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CheckoutRequest {
    @NotNull(message = "Shipping address ID is required")
    private UUID shippingAddressId;

    private String note;

    @NotBlank(message = "Payment method is required")
    private String paymentMethod; // COD, BANK_TRANSFER, etc.

    // Optional: For guest checkout
    private String guestId;
}

