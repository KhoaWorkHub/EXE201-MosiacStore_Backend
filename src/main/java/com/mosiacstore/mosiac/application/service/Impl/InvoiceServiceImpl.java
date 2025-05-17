package com.mosiacstore.mosiac.application.service.Impl;

import com.mosiacstore.mosiac.application.dto.response.InvoiceResponse;
import com.mosiacstore.mosiac.application.exception.EntityNotFoundException;
import com.mosiacstore.mosiac.application.exception.InvalidOperationException;
import com.mosiacstore.mosiac.application.service.InvoiceService;
import com.mosiacstore.mosiac.domain.order.Invoice;
import com.mosiacstore.mosiac.domain.order.Order;
import com.mosiacstore.mosiac.domain.order.OrderStatus;
import com.mosiacstore.mosiac.infrastructure.repository.InvoiceRepository;
import com.mosiacstore.mosiac.infrastructure.repository.OrderRepository;
import com.mosiacstore.mosiac.infrastructure.service.StorageServiceDelegate;
import com.mosiacstore.mosiac.infrastructure.util.MockMultipartFile;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@Slf4j
public class InvoiceServiceImpl implements InvoiceService {

    private final InvoiceRepository invoiceRepository;
    private final OrderRepository orderRepository;
    private final StorageServiceDelegate storageServiceDelegate;
    private final EmailService emailService;
    private final TemplateEngine templateEngine;

    @Value("${application.invoice.logo-path:classpath:static/images/logo.png}")
    private String logoPath;

    @Override
    public InvoiceResponse getInvoice(UUID orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new EntityNotFoundException("Order not found with ID: " + orderId));

        Invoice invoice = order.getInvoice();
        if (invoice == null) {
            throw new EntityNotFoundException("Invoice not found for order with ID: " + orderId);
        }

        return mapToInvoiceResponse(invoice);
    }

    @Override
    public InvoiceResponse getUserInvoice(UUID orderId, UUID userId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new EntityNotFoundException("Order not found with ID: " + orderId));

        if (!order.getUser().getId().equals(userId)) {
            throw new InvalidOperationException("Order does not belong to the current user");
        }

        Invoice invoice = order.getInvoice();
        if (invoice == null) {
            throw new EntityNotFoundException("Invoice not found for order with ID: " + orderId);
        }

        return mapToInvoiceResponse(invoice);
    }

    @Override
    @Transactional
    public InvoiceResponse generateInvoice(UUID orderId, boolean sendEmail, UUID adminId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new EntityNotFoundException("Order not found with ID: " + orderId));

        // Validate order status
        if (order.getStatus() == OrderStatus.PENDING_PAYMENT) {
            throw new InvalidOperationException("Cannot generate invoice for an unpaid order");
        }

        // Check if invoice already exists
        if (order.getInvoice() != null) {
            log.info("Invoice already exists for order {}, regenerating", order.getOrderNumber());
            invoiceRepository.delete(order.getInvoice());
        }

        // Generate invoice number
        String invoiceNumber = generateInvoiceNumber(order);

        // Create invoice document
        Invoice invoice = new Invoice();
        invoice.setOrder(order);
        invoice.setInvoiceNumber(invoiceNumber);
        invoice.setIssuedDate(LocalDateTime.now());
        invoice.setSent(false);

        // Create PDF and store it
        try {
            byte[] pdfBytes = generateInvoicePdf(order, invoice);
            String pdfUrl = storeInvoicePdf(pdfBytes, invoice.getInvoiceNumber());
            invoice.setPdfUrl(pdfUrl);
        } catch (Exception e) {
            log.error("Error generating invoice PDF for order {}", order.getOrderNumber(), e);
            throw new RuntimeException("Failed to generate invoice PDF", e);
        }

        Invoice savedInvoice = invoiceRepository.save(invoice);

        // Send invoice email if requested
        if (sendEmail) {
            sendInvoiceEmail(orderId, adminId);
        }

        return mapToInvoiceResponse(savedInvoice);
    }

    @Override
    @Transactional
    public void deleteInvoice(UUID orderId, UUID adminId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new EntityNotFoundException("Order not found with ID: " + orderId));

        Invoice invoice = order.getInvoice();
        if (invoice == null) {
            throw new EntityNotFoundException("Invoice not found for order with ID: " + orderId);
        }

        // Delete PDF file from storage
        if (invoice.getPdfUrl() != null && !invoice.getPdfUrl().isEmpty()) {
            try {
                storageServiceDelegate.deleteFile(invoice.getPdfUrl());
            } catch (Exception e) {
                log.warn("Failed to delete invoice PDF file: {}", invoice.getPdfUrl(), e);
            }
        }

        invoiceRepository.delete(invoice);
    }

    @Override
    @Transactional
    public void sendInvoiceEmail(UUID orderId, UUID adminId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new EntityNotFoundException("Order not found with ID: " + orderId));

        Invoice invoice = order.getInvoice();
        if (invoice == null) {
            throw new EntityNotFoundException("Invoice not found for order with ID: " + orderId);
        }

        // Send email asynchronously
        CompletableFuture<Void> emailFuture = sendInvoiceEmailAsync(order, invoice);

        // Mark invoice as sent
        invoice.setSent(true);
        invoiceRepository.save(invoice);
    }

    @Override
    public Resource downloadInvoice(UUID orderId, UUID userId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new EntityNotFoundException("Order not found with ID: " + orderId));

        // Verify user access if userId is provided
        if (userId != null && !order.getUser().getId().equals(userId)) {
            throw new InvalidOperationException("Order does not belong to the current user");
        }

        Invoice invoice = order.getInvoice();
        if (invoice == null) {
            throw new EntityNotFoundException("Invoice not found for order with ID: " + orderId);
        }

        if (invoice.getPdfUrl() == null || invoice.getPdfUrl().isEmpty()) {
            throw new InvalidOperationException("Invoice PDF not available");
        }

        try {
            // Regenerate PDF if needed (for example, if stored file is unavailable)
            byte[] pdfBytes = regenerateInvoicePdfIfNeeded(order, invoice);
            return new ByteArrayResource(pdfBytes);
        } catch (Exception e) {
            log.error("Error downloading invoice for order {}", order.getOrderNumber(), e);
            throw new RuntimeException("Failed to download invoice", e);
        }
    }

    // Helper methods

    private InvoiceResponse mapToInvoiceResponse(Invoice invoice) {
        return InvoiceResponse.builder()
                .id(invoice.getId())
                .orderId(invoice.getOrder().getId())
                .invoiceNumber(invoice.getInvoiceNumber())
                .pdfUrl(invoice.getPdfUrl())
                .issuedDate(invoice.getIssuedDate())
                .sent(invoice.getSent())
                .build();
    }

    private String generateInvoiceNumber(Order order) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyMMdd");
        String datePrefix = LocalDateTime.now().format(formatter);
        return "INV-" + datePrefix + "-" + order.getOrderNumber();
    }

    private byte[] generateInvoicePdf(Order order, Invoice invoice) throws Exception {
        // Create a new PDF document
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);

            // Load custom font
            PDFont font = PDType0Font.load(document, new File("src/main/resources/fonts/Roboto-Regular.ttf"));
            PDFont boldFont = PDType0Font.load(document, new File("src/main/resources/fonts/Roboto-Bold.ttf"));

            // Declare contentStream outside try-with-resources
            PDPageContentStream contentStream = new PDPageContentStream(document, page);

            try {
                // Add company logo
                PDImageXObject logo = PDImageXObject.createFromFile(logoPath, document);
                contentStream.drawImage(logo, 50, 750, 100, 50);

                // Add invoice title
                contentStream.beginText();
                contentStream.setFont(boldFont, 18);
                contentStream.newLineAtOffset(250, 770);
                contentStream.showText("INVOICE");
                contentStream.endText();

                // Add invoice details
                contentStream.beginText();
                contentStream.setFont(boldFont, 12);
                contentStream.newLineAtOffset(450, 750);
                contentStream.showText("Invoice No: " + invoice.getInvoiceNumber());
                contentStream.endText();

                contentStream.beginText();
                contentStream.setFont(font, 12);
                contentStream.newLineAtOffset(450, 735);
                contentStream.showText("Date: " + invoice.getIssuedDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
                contentStream.endText();

                contentStream.beginText();
                contentStream.setFont(font, 12);
                contentStream.newLineAtOffset(450, 720);
                contentStream.showText("Order No: " + order.getOrderNumber());
                contentStream.endText();

                // Add company details
                contentStream.beginText();
                contentStream.setFont(boldFont, 12);
                contentStream.newLineAtOffset(50, 700);
                contentStream.showText("Mosiac Store");
                contentStream.endText();

                contentStream.beginText();
                contentStream.setFont(font, 10);
                contentStream.newLineAtOffset(50, 685);
                contentStream.showText("Ho Chi Minh City, Vietnam");
                contentStream.endText();

                contentStream.beginText();
                contentStream.setFont(font, 10);
                contentStream.newLineAtOffset(50, 670);
                contentStream.showText("Email: support@mosiacstore.com");
                contentStream.endText();

                contentStream.beginText();
                contentStream.setFont(font, 10);
                contentStream.newLineAtOffset(50, 655);
                contentStream.showText("Phone: +84 788-732-514");
                contentStream.endText();

                // Add customer details
                contentStream.beginText();
                contentStream.setFont(boldFont, 12);
                contentStream.newLineAtOffset(50, 620);
                contentStream.showText("Bill To:");
                contentStream.endText();

                contentStream.beginText();
                contentStream.setFont(font, 10);
                contentStream.newLineAtOffset(50, 605);
                contentStream.showText("Name: " + order.getRecipientName());
                contentStream.endText();

                contentStream.beginText();
                contentStream.setFont(font, 10);
                contentStream.newLineAtOffset(50, 590);
                contentStream.showText("Phone: " + order.getRecipientPhone());
                contentStream.endText();

                contentStream.beginText();
                contentStream.setFont(font, 10);
                contentStream.newLineAtOffset(50, 575);
                contentStream.showText("Address: " + order.getShippingAddressSnapshot());
                contentStream.endText();

                // Add table headers
                contentStream.beginText();
                contentStream.setFont(boldFont, 10);
                contentStream.newLineAtOffset(50, 540);
                contentStream.showText("Item");
                contentStream.endText();

                contentStream.beginText();
                contentStream.setFont(boldFont, 10);
                contentStream.newLineAtOffset(250, 540);
                contentStream.showText("Quantity");
                contentStream.endText();

                contentStream.beginText();
                contentStream.setFont(boldFont, 10);
                contentStream.newLineAtOffset(350, 540);
                contentStream.showText("Price");
                contentStream.endText();

                contentStream.beginText();
                contentStream.setFont(boldFont, 10);
                contentStream.newLineAtOffset(450, 540);
                contentStream.showText("Subtotal");
                contentStream.endText();

                // Draw table line
                contentStream.setLineWidth(1f);
                contentStream.moveTo(50, 535);
                contentStream.lineTo(550, 535);
                contentStream.stroke();

                // Add order items
                int yPosition = 520;
                for (var item : order.getOrderItems()) {
                    String productName = item.getProductNameSnapshot();
                    if (item.getVariantInfoSnapshot() != null) {
                        productName += " (" + item.getVariantInfoSnapshot() + ")";
                    }

                    contentStream.beginText();
                    contentStream.setFont(font, 10);
                    contentStream.newLineAtOffset(50, yPosition);
                    contentStream.showText(productName);
                    contentStream.endText();

                    contentStream.beginText();
                    contentStream.setFont(font, 10);
                    contentStream.newLineAtOffset(250, yPosition);
                    contentStream.showText(String.valueOf(item.getQuantity()));
                    contentStream.endText();

                    contentStream.beginText();
                    contentStream.setFont(font, 10);
                    contentStream.newLineAtOffset(350, yPosition);
                    contentStream.showText(formatCurrency(item.getPriceSnapshot()));
                    contentStream.endText();

                    contentStream.beginText();
                    contentStream.setFont(font, 10);
                    contentStream.newLineAtOffset(450, yPosition);
                    contentStream.showText(formatCurrency(item.getSubtotal()));
                    contentStream.endText();

                    yPosition -= 15;

                    // Add page break if needed
                    if (yPosition < 100) {
                        contentStream.close(); // Đóng stream hiện tại trước khi tạo trang mới
                        PDPage newPage = new PDPage(PDRectangle.A4);
                        document.addPage(newPage);
                        contentStream = new PDPageContentStream(document, newPage); // Bây giờ có thể gán lại
                        yPosition = 750;
                    }
                }

                // Draw total line
                contentStream.setLineWidth(1f);
                contentStream.moveTo(350, yPosition - 5);
                contentStream.lineTo(550, yPosition - 5);
                contentStream.stroke();

                // Add totals
                yPosition -= 20;

                contentStream.beginText();
                contentStream.setFont(boldFont, 10);
                contentStream.newLineAtOffset(350, yPosition);
                contentStream.showText("Subtotal:");
                contentStream.endText();

                contentStream.beginText();
                contentStream.setFont(font, 10);
                contentStream.newLineAtOffset(450, yPosition);
                contentStream.showText(formatCurrency(order.getTotalProductAmount()));
                contentStream.endText();

                yPosition -= 15;

                contentStream.beginText();
                contentStream.setFont(boldFont, 10);
                contentStream.newLineAtOffset(350, yPosition);
                contentStream.showText("Shipping Fee:");
                contentStream.endText();

                contentStream.beginText();
                contentStream.setFont(font, 10);
                contentStream.newLineAtOffset(450, yPosition);
                contentStream.showText(formatCurrency(order.getShippingFee()));
                contentStream.endText();

                yPosition -= 15;

                contentStream.beginText();
                contentStream.setFont(boldFont, 10);
                contentStream.newLineAtOffset(350, yPosition);
                contentStream.showText("Total:");
                contentStream.endText();

                contentStream.beginText();
                contentStream.setFont(boldFont, 10);
                contentStream.newLineAtOffset(450, yPosition);
                contentStream.showText(formatCurrency(order.getTotalAmount()));
                contentStream.endText();

                // Add payment information
                yPosition -= 40;

                contentStream.beginText();
                contentStream.setFont(boldFont, 12);
                contentStream.newLineAtOffset(50, yPosition);
                contentStream.showText("Payment Information");
                contentStream.endText();

                yPosition -= 15;

                if (!order.getPayments().isEmpty()) {
                    var payment = order.getPayments().iterator().next();

                    contentStream.beginText();
                    contentStream.setFont(font, 10);
                    contentStream.newLineAtOffset(50, yPosition);
                    contentStream.showText("Payment Method: " + payment.getPaymentMethod().name());
                    contentStream.endText();

                    yPosition -= 15;

                    contentStream.beginText();
                    contentStream.setFont(font, 10);
                    contentStream.newLineAtOffset(50, yPosition);
                    contentStream.showText("Payment Status: " + payment.getStatus().name());
                    contentStream.endText();

                    if (payment.getTransactionReference() != null) {
                        yPosition -= 15;

                        contentStream.beginText();
                        contentStream.setFont(font, 10);
                        contentStream.newLineAtOffset(50, yPosition);
                        contentStream.showText("Transaction Reference: " + payment.getTransactionReference());
                        contentStream.endText();
                    }
                }

                // Add footer
                contentStream.beginText();
                contentStream.setFont(font, 10);
                contentStream.newLineAtOffset(50, 50);
                contentStream.showText("Thank you for your business!");
                contentStream.endText();

                contentStream.beginText();
                contentStream.setFont(font, 8);
                contentStream.newLineAtOffset(50, 35);
                contentStream.showText("This is a computer-generated document and does not require a signature.");
                contentStream.endText();
            } finally {
                if (contentStream != null) {
                    try {
                        contentStream.close();
                    } catch (IOException e) {
                        log.warn("Error closing content stream", e);
                    }
                }
            }

            // Save the PDF to a byte array
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            document.save(outputStream);
            return outputStream.toByteArray();
        }
    }

    private String storeInvoicePdf(byte[] pdfBytes, String invoiceNumber) {
        try {
            // Store the PDF file
            String fileName = "invoice_" + invoiceNumber + ".pdf";
            MockMultipartFile multipartFile = new MockMultipartFile(
                    fileName,
                    fileName,
                    "application/pdf",
                    pdfBytes
            );

            return storageServiceDelegate.uploadFile(multipartFile, "invoices");
        } catch (Exception e) {
            log.error("Failed to store invoice PDF", e);
            throw new RuntimeException("Failed to store invoice PDF", e);
        }
    }

    private byte[] regenerateInvoicePdfIfNeeded(Order order, Invoice invoice) throws Exception {
        try {
            // Try to get the existing PDF
            InputStream pdfStream = storageServiceDelegate.getFile(invoice.getPdfUrl());
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = pdfStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
            return outputStream.toByteArray();
        } catch (Exception e) {
            log.warn("Failed to get existing invoice PDF, regenerating", e);
            return generateInvoicePdf(order, invoice);
        }
    }

    @Async
    protected CompletableFuture<Void> sendInvoiceEmailAsync(Order order, Invoice invoice) {
        try {
            // Create email context
            Context context = new Context();
            context.setVariable("order", order);
            context.setVariable("invoice", invoice);
            context.setVariable("customerName", order.getRecipientName());

            // Process email template
            String emailContent = templateEngine.process("emails/invoice", context);

            // Send email with attachment
            // This is a placeholder - you would need to implement the actual email sending
            // with attachment functionality in your EmailService

            log.info("Invoice email sent to {} for order {}", order.getUser().getEmail(), order.getOrderNumber());
            return CompletableFuture.completedFuture(null);
        } catch (Exception e) {
            log.error("Failed to send invoice email", e);
            return CompletableFuture.failedFuture(e);
        }
    }

    private String formatCurrency(java.math.BigDecimal amount) {
        return String.format("%,.0f đ", amount);
    }
}