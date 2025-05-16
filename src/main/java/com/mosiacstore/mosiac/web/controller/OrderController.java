package com.mosiacstore.mosiac.web.controller;

import com.mosiacstore.mosiac.application.dto.request.CheckoutRequest;
import com.mosiacstore.mosiac.application.dto.request.PaymentConfirmationRequest;
import com.mosiacstore.mosiac.application.dto.response.CheckoutResponse;
import com.mosiacstore.mosiac.application.dto.response.OrderResponse;
import com.mosiacstore.mosiac.application.dto.response.PageResponse;
import com.mosiacstore.mosiac.application.dto.response.PaymentResponse;
import com.mosiacstore.mosiac.application.service.OrderService;
import com.mosiacstore.mosiac.application.service.PaymentService;
import com.mosiacstore.mosiac.infrastructure.security.CustomUserDetail;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.UUID;

@RestController
@Slf4j
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "Orders", description = "Order and Checkout API")
public class OrderController {

    private final OrderService orderService;
    private final PaymentService paymentService;


    @Operation(
            summary = "Get user orders",
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @GetMapping("/user/orders")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PageResponse<OrderResponse>> getUserOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt,desc") String sort,
            @AuthenticationPrincipal CustomUserDetail currentUser) {
        return ResponseEntity.ok(orderService.getUserOrders(currentUser.getUser().getId(), page, size, sort));
    }

    @Operation(
            summary = "Get order by ID",
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @GetMapping("/user/orders/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<OrderResponse> getOrderById(
            @PathVariable UUID id,
            @AuthenticationPrincipal CustomUserDetail currentUser) {
        return ResponseEntity.ok(orderService.getOrderById(id, currentUser.getUser().getId()));
    }

    @Operation(
            summary = "Get order by order number",
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @GetMapping("/user/orders/number/{orderNumber}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<OrderResponse> getOrderByNumber(
            @PathVariable String orderNumber,
            @AuthenticationPrincipal CustomUserDetail currentUser) {
        return ResponseEntity.ok(orderService.getOrderByNumber(orderNumber, currentUser.getUser().getId()));
    }

    @Operation(
            summary = "Checkout",
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @PostMapping("/checkout")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<CheckoutResponse> checkout(
            @Valid @RequestBody CheckoutRequest request,
            @AuthenticationPrincipal CustomUserDetail currentUser) {
        CheckoutResponse response = orderService.checkout(request, currentUser.getUser().getId());
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @Operation(
            summary = "Cancel order",
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @PutMapping("/user/orders/{id}/cancel")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<OrderResponse> cancelOrder(
            @PathVariable UUID id,
            @RequestParam String reason,
            @AuthenticationPrincipal CustomUserDetail currentUser) {
        return ResponseEntity.ok(orderService.cancelOrder(id, reason, currentUser.getUser().getId()));
    }

    @Operation(
            summary = "Confirm payment (for bank transfer)",
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @PostMapping("/user/orders/payment/confirm")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PaymentResponse> confirmPayment(
            @Valid @RequestBody PaymentConfirmationRequest request,
            @AuthenticationPrincipal CustomUserDetail currentUser) {
        return ResponseEntity.ok(paymentService.confirmPayment(request, currentUser.getUser().getId()));
    }

    // Admin endpoints
    @Operation(
            summary = "Get all orders (admin)",
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @GetMapping("/admin/orders")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PageResponse<OrderResponse>> getAllOrders(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) UUID userId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt,desc") String sort,
            @AuthenticationPrincipal CustomUserDetail currentUser) {
        return ResponseEntity.ok(orderService.getAllOrders(keyword, status, userId, startDate, endDate, page, size, sort));
    }

    @Operation(
            summary = "Update order status (admin)",
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @PutMapping("/admin/orders/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<OrderResponse> updateOrderStatus(
            @PathVariable UUID id,
            @RequestParam String status,
            @RequestParam(required = false) String adminNote,
            @AuthenticationPrincipal CustomUserDetail currentUser) {

        log.info("Admin {} updating order {} status to {} with note: {}",
                currentUser.getUser().getEmail(), id, status, adminNote);

        OrderResponse response = orderService.updateOrderStatus(id, status, adminNote, currentUser.getUser().getId());

        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Validate payment (admin)",
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @PutMapping("/admin/payments/{id}/validate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PaymentResponse> validatePayment(
            @PathVariable UUID id,
            @RequestParam boolean isValid,
            @RequestParam(required = false) String adminNote,
            @AuthenticationPrincipal CustomUserDetail currentUser) {
        return ResponseEntity.ok(paymentService.validatePayment(id, isValid, adminNote, currentUser.getUser().getId()));
    }

    @Operation(
            summary = "Mark payment as failed (admin)",
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @PutMapping("/admin/payments/{id}/fail")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PaymentResponse> markPaymentAsFailed(
            @PathVariable UUID id,
            @RequestParam String reason,
            @AuthenticationPrincipal CustomUserDetail currentUser) {
        return ResponseEntity.ok(paymentService.markPaymentAsFailed(id, reason, currentUser.getUser().getId()));
    }

    @Operation(
            summary = "Refund payment (admin)",
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @PutMapping("/admin/payments/{id}/refund")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PaymentResponse> refundPayment(
            @PathVariable UUID id,
            @RequestParam String reason,
            @AuthenticationPrincipal CustomUserDetail currentUser) {
        return ResponseEntity.ok(paymentService.refundPayment(id, reason, currentUser.getUser().getId()));
    }
}