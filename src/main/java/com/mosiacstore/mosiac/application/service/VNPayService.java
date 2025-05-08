package com.mosiacstore.mosiac.application.service;

import com.mosiacstore.mosiac.application.dto.response.PaymentUrlResponse;
import com.mosiacstore.mosiac.domain.order.Order;

import java.util.Map;

public interface VNPayService {
    PaymentUrlResponse createPaymentUrl(Order order, String ipAddress);
    boolean validatePaymentResponse(Map<String, String> vnpParams);
}