package com.mosiacstore.mosiac.application.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvoiceResponse {
    private UUID id;
    private UUID orderId;
    private String invoiceNumber;
    private String pdfUrl;
    private LocalDateTime issuedDate;
    private Boolean sent;
}