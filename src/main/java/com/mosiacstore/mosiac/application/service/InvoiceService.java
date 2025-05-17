package com.mosiacstore.mosiac.application.service;

import com.mosiacstore.mosiac.application.dto.response.ApiResponse;
import com.mosiacstore.mosiac.application.dto.response.InvoiceResponse;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public interface InvoiceService {

    /**
     * Get invoice for a specific order (admin access)
     */
    InvoiceResponse getInvoice(UUID orderId);

    /**
     * Get invoice for a specific order (user access)
     */
    InvoiceResponse getUserInvoice(UUID orderId, UUID userId);

    /**
     * Generate a new invoice for an order
     */
    InvoiceResponse generateInvoice(UUID orderId, boolean sendEmail, UUID adminId);

    /**
     * Delete an invoice
     */
    void deleteInvoice(UUID orderId, UUID adminId);

    /**
     * Send invoice email to the customer
     */
    void sendInvoiceEmail(UUID orderId, UUID adminId);

    /**
     * Download invoice as PDF
     */
    Resource downloadInvoice(UUID orderId, UUID userId);
}