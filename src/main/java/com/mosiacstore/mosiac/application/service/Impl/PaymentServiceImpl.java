package com.mosiacstore.mosiac.application.service.Impl;

import com.mosiacstore.mosiac.application.dto.request.PaymentConfirmationRequest;
import com.mosiacstore.mosiac.application.dto.response.PaymentResponse;
import com.mosiacstore.mosiac.application.exception.EntityNotFoundException;
import com.mosiacstore.mosiac.application.exception.InvalidOperationException;
import com.mosiacstore.mosiac.application.service.PaymentService;
import com.mosiacstore.mosiac.domain.order.Order;
import com.mosiacstore.mosiac.domain.order.OrderStatus;
import com.mosiacstore.mosiac.domain.payment.Payment;
import com.mosiacstore.mosiac.domain.payment.PaymentMethod;
import com.mosiacstore.mosiac.domain.payment.PaymentStatus;
import com.mosiacstore.mosiac.domain.user.User;
import com.mosiacstore.mosiac.infrastructure.repository.OrderRepository;
import com.mosiacstore.mosiac.infrastructure.repository.PaymentRepository;
import com.mosiacstore.mosiac.infrastructure.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;

    @Override
    public PaymentResponse getPaymentByOrderId(UUID orderId) {
        Payment payment = paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new EntityNotFoundException("Payment not found for order with ID: " + orderId));

        return mapToPaymentResponse(payment);
    }

    @Override
    @Transactional
    public PaymentResponse confirmPayment(PaymentConfirmationRequest request, UUID userId) {
        Order order = orderRepository.findById(request.getOrderId())
                .orElseThrow(() -> new EntityNotFoundException("Order not found with ID: " + request.getOrderId()));

        if (!order.getUser().getId().equals(userId)) {
            throw new InvalidOperationException("Order does not belong to the current user");
        }

        Payment payment = paymentRepository.findByOrderId(request.getOrderId())
                .orElseThrow(() -> new EntityNotFoundException("Payment not found for order with ID: " + request.getOrderId()));

        if (payment.getStatus() != PaymentStatus.PENDING) {
            throw new InvalidOperationException("Payment is already processed");
        }

        if (payment.getPaymentMethod() == PaymentMethod.COD) {
            throw new InvalidOperationException("COD payments do not need confirmation");
        }

        // For bank transfer, update payment info
        if (payment.getPaymentMethod() == PaymentMethod.BANK_TRANSFER) {
            payment.setTransactionReference(request.getTransactionReference());
            payment.setBankName(request.getBankName());
            payment.setBankAccountNumber(request.getAccountNumber());
            payment.setPaymentNote(request.getPaymentNote());
            payment.setPaymentDate(LocalDateTime.now());

            // Update status to pending admin verification
            payment.setStatus(PaymentStatus.PENDING);

            // The order stays in PENDING_PAYMENT until admin verifies
        }

        Payment updatedPayment = paymentRepository.save(payment);
        return mapToPaymentResponse(updatedPayment);
    }

    @Override
    @Transactional
    public PaymentResponse validatePayment(UUID paymentId, boolean isValid, String adminNote, UUID adminId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new EntityNotFoundException("Payment not found with ID: " + paymentId));

        User admin = userRepository.findById(adminId)
                .orElseThrow(() -> new EntityNotFoundException("Admin user not found with ID: " + adminId));

        Order order = payment.getOrder();

        if (payment.getStatus() != PaymentStatus.PENDING) {
            throw new InvalidOperationException("Payment is already processed");
        }

        if (isValid) {
            // Mark payment as completed
            payment.setStatus(PaymentStatus.COMPLETED);
            payment.setPaymentDate(LocalDateTime.now());
            payment.setAdminNote(adminNote);
            payment.setAdmin(admin);

            // Update order status
            order.setStatus(OrderStatus.PROCESSING);
        } else {
            // Mark payment as failed
            payment.setStatus(PaymentStatus.FAILED);
            payment.setAdminNote(adminNote);
            payment.setAdmin(admin);
        }

        orderRepository.save(order);
        Payment updatedPayment = paymentRepository.save(payment);
        return mapToPaymentResponse(updatedPayment);
    }

    @Override
    @Transactional
    public PaymentResponse markPaymentAsFailed(UUID paymentId, String reason, UUID adminId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new EntityNotFoundException("Payment not found with ID: " + paymentId));

        User admin = userRepository.findById(adminId)
                .orElseThrow(() -> new EntityNotFoundException("Admin user not found with ID: " + adminId));

        if (payment.getStatus() != PaymentStatus.PENDING) {
            throw new InvalidOperationException("Payment is already processed");
        }

        payment.setStatus(PaymentStatus.FAILED);
        payment.setAdminNote(reason);
        payment.setAdmin(admin);

        Payment updatedPayment = paymentRepository.save(payment);
        return mapToPaymentResponse(updatedPayment);
    }

    @Override
    @Transactional
    public PaymentResponse refundPayment(UUID paymentId, String reason, UUID adminId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new EntityNotFoundException("Payment not found with ID: " + paymentId));

        User admin = userRepository.findById(adminId)
                .orElseThrow(() -> new EntityNotFoundException("Admin user not found with ID: " + adminId));

        if (payment.getStatus() != PaymentStatus.COMPLETED) {
            throw new InvalidOperationException("Can only refund completed payments");
        }

        payment.setStatus(PaymentStatus.REFUNDED);
        payment.setAdminNote(reason);
        payment.setAdmin(admin);

        // Update order status to cancelled
        Order order = payment.getOrder();
        order.setStatus(OrderStatus.CANCELLED);
        order.setCancelledReason("Payment refunded: " + reason);
        orderRepository.save(order);

        Payment updatedPayment = paymentRepository.save(payment);
        return mapToPaymentResponse(updatedPayment);
    }

    // Helper method
    private PaymentResponse mapToPaymentResponse(Payment payment) {
        return PaymentResponse.builder()
                .id(payment.getId())
                .paymentMethod(payment.getPaymentMethod())
                .amount(payment.getAmount())
                .status(payment.getStatus())
                .transactionReference(payment.getTransactionReference())
                .paymentDate(payment.getPaymentDate())
                .bankName(payment.getBankName())
                .bankAccountNumber(payment.getBankAccountNumber())
                .paymentNote(payment.getPaymentNote())
                .build();
    }
}