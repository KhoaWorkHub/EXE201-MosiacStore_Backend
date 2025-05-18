package com.mosiacstore.mosiac.web.controller;

import com.mosiacstore.mosiac.application.dto.response.ApiResponse;
import com.mosiacstore.mosiac.application.exception.EntityNotFoundException;
import com.mosiacstore.mosiac.application.service.Impl.EmailService;
import com.mosiacstore.mosiac.domain.order.Order;
import com.mosiacstore.mosiac.infrastructure.repository.OrderRepository;
import com.mosiacstore.mosiac.infrastructure.security.CustomUserDetail;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/orders")
@PreAuthorize("hasRole('ADMIN')")
public class AdminShippingController {

    private final OrderRepository orderRepository;
    private final EmailService emailService;

    @PostMapping("/{id}/shipping-fee")
    public ResponseEntity<ApiResponse> setShippingFee(
            @PathVariable UUID id,
            @RequestParam BigDecimal shippingFee,
            @AuthenticationPrincipal CustomUserDetail currentUser) {

        // Get the order
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Order not found with ID: " + id));

        // Update shipping fee
        order.setShippingFee(shippingFee);

        // Recalculate total amount
        order.setTotalAmount(order.getTotalProductAmount().add(shippingFee));

        // Save the updated order
        orderRepository.save(order);

        // Send email notification
        emailService.sendShippingFeeEmail(order, shippingFee);

        return ResponseEntity.ok(new ApiResponse(true, "Shipping fee updated and notification sent successfully"));
    }
}