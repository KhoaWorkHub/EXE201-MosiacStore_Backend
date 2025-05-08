package com.mosiacstore.mosiac.application.dto.response;


import com.mosiacstore.mosiac.domain.payment.PaymentMethod;
import com.mosiacstore.mosiac.domain.payment.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentResponse {
    private UUID id;
    private PaymentMethod paymentMethod;
    private BigDecimal amount;
    private PaymentStatus status;
    private String transactionReference;
    private LocalDateTime paymentDate;
    private String bankName;
    private String bankAccountNumber;
    private String paymentNote;
}