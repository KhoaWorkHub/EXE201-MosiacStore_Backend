package com.mosiacstore.mosiac.application.dto.response;

import com.mosiacstore.mosiac.application.dto.UserDto;
import com.mosiacstore.mosiac.domain.order.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderDetailResponse {
    private UUID id;
    private String orderNumber;
    private OrderStatus status;
    private BigDecimal totalProductAmount;
    private BigDecimal shippingFee;
    private BigDecimal totalAmount;
    private String recipientName;
    private String recipientPhone;
    private String shippingAddressSnapshot;
    private String note;
    private String adminNote;
    private LocalDateTime paymentDue;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<OrderItemResponse> orderItems;
    private PaymentResponse payment;
    private InvoiceResponse invoice;
    private UserDto user;
}