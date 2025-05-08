package com.mosiacstore.mosiac.application.dto.request;

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
public class PaymentConfirmationRequest {
    @NotNull(message = "Order ID is required")
    private UUID orderId;

    // Fields for bank transfer
    private String transactionReference;
    private String bankName;
    private String accountNumber;
    private String paymentNote;
}
