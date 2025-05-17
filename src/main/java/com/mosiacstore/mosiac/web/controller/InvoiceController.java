package com.mosiacstore.mosiac.web.controller;

import com.mosiacstore.mosiac.application.dto.response.ApiResponse;
import com.mosiacstore.mosiac.application.dto.response.InvoiceResponse;
import com.mosiacstore.mosiac.application.service.InvoiceService;
import com.mosiacstore.mosiac.infrastructure.security.CustomUserDetail;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "Invoices", description = "Invoice API")
public class InvoiceController {

    private final InvoiceService invoiceService;

    // User endpoints

    @Operation(
            summary = "Get invoice for an order (user)",
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @GetMapping("/user/orders/{orderId}/invoice")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<InvoiceResponse> getUserInvoice(
            @PathVariable UUID orderId,
            @AuthenticationPrincipal CustomUserDetail currentUser) {
        return ResponseEntity.ok(invoiceService.getUserInvoice(orderId, currentUser.getUser().getId()));
    }

    @Operation(
            summary = "Download invoice PDF (user)",
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @GetMapping("/user/orders/{orderId}/invoice/download")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Resource> downloadUserInvoice(
            @PathVariable UUID orderId,
            @AuthenticationPrincipal CustomUserDetail currentUser) {
        InvoiceResponse invoice = invoiceService.getUserInvoice(orderId, currentUser.getUser().getId());
        Resource resource = invoiceService.downloadInvoice(orderId, currentUser.getUser().getId());

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"invoice_" + invoice.getInvoiceNumber() + ".pdf\"")
                .header(HttpHeaders.CONTENT_TYPE, "application/pdf")
                .body(resource);
    }

    // Admin endpoints

    @Operation(
            summary = "Get invoice for an order (admin)",
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @GetMapping("/admin/orders/{orderId}/invoice")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<InvoiceResponse> getInvoice(
            @PathVariable UUID orderId,
            @AuthenticationPrincipal CustomUserDetail currentUser) {
        return ResponseEntity.ok(invoiceService.getInvoice(orderId));
    }

    @Operation(
            summary = "Generate invoice for an order (admin)",
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @PostMapping("/admin/orders/{orderId}/invoice")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<InvoiceResponse> generateInvoice(
            @PathVariable UUID orderId,
            @RequestParam(required = false, defaultValue = "false") Boolean sendEmail,
            @AuthenticationPrincipal CustomUserDetail currentUser) {
        return ResponseEntity.ok(invoiceService.generateInvoice(orderId, sendEmail, currentUser.getUser().getId()));
    }

    @Operation(
            summary = "Download invoice PDF (admin)",
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @GetMapping("/admin/orders/{orderId}/invoice/download")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Resource> downloadInvoice(
            @PathVariable UUID orderId,
            @AuthenticationPrincipal CustomUserDetail currentUser) {
        InvoiceResponse invoice = invoiceService.getInvoice(orderId);
        Resource resource = invoiceService.downloadInvoice(orderId, null);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"invoice_" + invoice.getInvoiceNumber() + ".pdf\"")
                .header(HttpHeaders.CONTENT_TYPE, "application/pdf")
                .body(resource);
    }

    @Operation(
            summary = "Delete an invoice (admin)",
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @DeleteMapping("/admin/orders/{orderId}/invoice")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse> deleteInvoice(
            @PathVariable UUID orderId,
            @AuthenticationPrincipal CustomUserDetail currentUser) {
        invoiceService.deleteInvoice(orderId, currentUser.getUser().getId());
        return ResponseEntity.ok(new ApiResponse(true, "Invoice deleted successfully"));
    }

    @Operation(
            summary = "Send invoice email (admin)",
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @PostMapping("/admin/orders/{orderId}/invoice/send")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse> sendInvoiceEmail(
            @PathVariable UUID orderId,
            @AuthenticationPrincipal CustomUserDetail currentUser) {
        invoiceService.sendInvoiceEmail(orderId, currentUser.getUser().getId());
        return ResponseEntity.ok(new ApiResponse(true, "Invoice email sent successfully"));
    }
}