package com.mosiacstore.mosiac.application.service;

import com.mosiacstore.mosiac.application.dto.request.PaymentConfirmationRequest;
import com.mosiacstore.mosiac.application.dto.response.PaymentResponse;

import java.util.UUID;

public interface PaymentService {
    PaymentResponse getPaymentByOrderId(UUID orderId);
    PaymentResponse confirmPayment(PaymentConfirmationRequest request, UUID userId);

    // Admin functions
    PaymentResponse validatePayment(UUID paymentId, boolean isValid, String adminNote, UUID adminId);
    PaymentResponse markPaymentAsFailed(UUID paymentId, String reason, UUID adminId);
    PaymentResponse refundPayment(UUID paymentId, String reason, UUID adminId);
}