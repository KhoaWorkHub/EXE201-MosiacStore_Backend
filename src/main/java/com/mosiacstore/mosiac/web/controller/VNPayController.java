package com.mosiacstore.mosiac.web.controller;

import com.mosiacstore.mosiac.application.dto.response.ApiResponse;
import com.mosiacstore.mosiac.application.dto.response.PaymentUrlResponse;
import com.mosiacstore.mosiac.application.service.OrderService;
import com.mosiacstore.mosiac.application.service.PaymentService;
import com.mosiacstore.mosiac.application.service.VNPayService;
import com.mosiacstore.mosiac.domain.order.Order;
import com.mosiacstore.mosiac.domain.order.OrderStatus;
import com.mosiacstore.mosiac.domain.payment.Payment;
import com.mosiacstore.mosiac.domain.payment.PaymentStatus;
import com.mosiacstore.mosiac.infrastructure.repository.OrderRepository;
import com.mosiacstore.mosiac.infrastructure.repository.PaymentRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/payment/vnpay")
@RequiredArgsConstructor
@Tag(name = "VNPay", description = "VNPay Payment API")
public class VNPayController {

    private final VNPayService vnPayService;
    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;

    @Operation(summary = "Create VNPay payment URL")
    @GetMapping("/create-payment/{orderId}")
    public ResponseEntity<PaymentUrlResponse> createPayment(
            @PathVariable UUID orderId,
            HttpServletRequest request) {

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        String ipAddress = request.getRemoteAddr();
        PaymentUrlResponse response = vnPayService.createPaymentUrl(order, ipAddress);

        return ResponseEntity.ok(response);
    }

    @Operation(summary = "VNPay payment callback")
    @Transactional
    @GetMapping("/payment-callback")
    public ResponseEntity<ApiResponse> paymentCallback(HttpServletRequest request) {
        Map<String, String> vnpParams = new HashMap<>();
        Enumeration<String> parameterNames = request.getParameterNames();

        while (parameterNames.hasMoreElements()) {
            String paramName = parameterNames.nextElement();
            String paramValue = request.getParameter(paramName);
            if (paramName.startsWith("vnp_")) {
                vnpParams.put(paramName, paramValue);
            }
        }

        // Validate parameters
        if (vnPayService.validatePaymentResponse(vnpParams)) {
            String vnpResponseCode = vnpParams.get("vnp_ResponseCode");
            String orderNumber = vnpParams.get("vnp_TxnRef");

            Order order = orderRepository.findByOrderNumber(orderNumber)
                    .orElseThrow(() -> new RuntimeException("Order not found"));

            Payment payment = paymentRepository.findByOrderId(order.getId())
                    .orElseThrow(() -> new RuntimeException("Payment not found"));

            if ("00".equals(vnpResponseCode)) {
                // Payment successful
                payment.setStatus(PaymentStatus.COMPLETED);
                payment.setTransactionReference(vnpParams.get("vnp_TransactionNo"));
                payment.setPaymentDate(LocalDateTime.now());
                paymentRepository.save(payment);

                // Update order status
                order.setStatus(OrderStatus.PROCESSING);
                orderRepository.save(order);
                System.out.println("VNPay callback received with params: " + vnpParams);

                return ResponseEntity.ok(new ApiResponse(true, "Payment successful"));

            } else {
                // Payment failed
                payment.setStatus(PaymentStatus.FAILED);
                payment.setPaymentNote("VNPay error: " + vnpResponseCode);
                paymentRepository.save(payment);

                return ResponseEntity.ok(new ApiResponse(false, "Payment failed: " + vnpResponseCode));
            }
        } else {
            return ResponseEntity.badRequest().body(new ApiResponse(false, "Invalid payment data"));
        }
    }
}