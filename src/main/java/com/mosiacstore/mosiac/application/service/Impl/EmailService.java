package com.mosiacstore.mosiac.application.service.Impl;

import com.mosiacstore.mosiac.domain.order.Order;
import com.mosiacstore.mosiac.infrastructure.service.ConcurrencyManager;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.io.File;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Service for sending email notifications
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;
    private final ConcurrencyManager concurrencyManager;

    @Value("${spring.mail.username:tkhoa7815@gmail.com}")
    private String senderEmail;

    @Value("${application.front-end-url:https://mosiacstore.vercel.app}")
    private String frontendUrl;

    /**
     * Send order paid confirmation email
     * @param order The paid order
     * @return CompletableFuture for async operation
     */
    @Async
    public CompletableFuture<Void> sendOrderPaidEmail(Order order) {
        String subject = "Payment Confirmed for Order #" + order.getOrderNumber();
        String template = "order-paid";

        Map<String, Object> variables = new HashMap<>();
        variables.put("order", order);
        variables.put("frontendUrl", frontendUrl);
        variables.put("supportEmail", "support@mosiacstore.com");
        variables.put("contactPhone", "+84 788-732-514");

        // Add payment information if available
        if (order.getPayments() != null && !order.getPayments().isEmpty()) {
            variables.put("payment", order.getPayments().iterator().next());
        }

        return sendEmailWithTemplate(order.getUser().getEmail(), subject, template, variables);
    }

    /**
     * Send order confirmation email
     * @param order The order to confirm
     * @return CompletableFuture for async operation
     */
    @Async
    public CompletableFuture<Void> sendOrderConfirmationEmail(Order order) {
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

            helper.setFrom(senderEmail);
            helper.setTo(order.getUser().getEmail());
            helper.setSubject("Your Order #" + order.getOrderNumber() + " has been received");

            // Add template variables
            Context context = new Context(Locale.getDefault());
            context.setVariable("order", order);
            context.setVariable("frontendUrl", frontendUrl);
            context.setVariable("supportEmail", "support@mosiacstore.com");
            context.setVariable("contactPhone", "+84 788-732-514");
            context.setVariable("shippingNotice", "Phí vận chuyển sẽ được tính riêng sau khi đơn hàng được xác nhận.");
            context.setVariable("shippingPaymentInfo", "Chúng tôi sẽ gửi email thông báo phí vận chuyển cùng mã QR để thanh toán.");

            // Handle payment
            if (order.getPayments() != null && !order.getPayments().isEmpty()) {
                context.setVariable("payment", order.getPayments().iterator().next());
            }

            // Process template
            String htmlContent = templateEngine.process("emails/order-confirmation", context);
            helper.setText(htmlContent, true);

            // Add the logo as an inline attachment
            FileSystemResource logo = new FileSystemResource(new File("src/main/resources/static/images/logo.png"));
            helper.addInline("logoImage", logo);

            mailSender.send(mimeMessage);
            log.info("Order confirmation email sent to {}", order.getUser().getEmail());
            return CompletableFuture.completedFuture(null);
        } catch (MessagingException e) {
            log.error("Failed to send order confirmation email", e);
            return CompletableFuture.failedFuture(e);
        }
    }

    /**
     * Send order processing email
     * @param order The order being processed
     * @return CompletableFuture for async operation
     */
    @Async
    public CompletableFuture<Void> sendOrderProcessingEmail(Order order) {
        String subject = "Your Order #" + order.getOrderNumber() + " is being processed";
        String template = "order-processing";

        Map<String, Object> variables = new HashMap<>();
        variables.put("order", order);
        variables.put("frontendUrl", frontendUrl);
        variables.put("supportEmail", "support@mosiacstore.com");
        variables.put("contactPhone", "+84 788-732-514");

        return sendEmailWithTemplate(order.getUser().getEmail(), subject, template, variables);
    }

    /**
     * Send order shipping email
     * @param order The order being shipped
     * @return CompletableFuture for async operation
     */
    @Async
    public CompletableFuture<Void> sendOrderShippingEmail(Order order) {
        String subject = "Your Order #" + order.getOrderNumber() + " has been shipped!";
        String template = "order-shipping";

        Map<String, Object> variables = new HashMap<>();
        variables.put("order", order);
        variables.put("frontendUrl", frontendUrl);
        variables.put("supportEmail", "support@mosiacstore.com");
        variables.put("contactPhone", "+84 788-732-514");

        return sendEmailWithTemplate(order.getUser().getEmail(), subject, template, variables);
    }

    /**
     * Send order delivered email
     * @param order The delivered order
     * @return CompletableFuture for async operation
     */
    @Async
    public CompletableFuture<Void> sendOrderDeliveredEmail(Order order) {
        String subject = "Your Order #" + order.getOrderNumber() + " has been delivered";
        String template = "order-delivered";

        Map<String, Object> variables = new HashMap<>();
        variables.put("order", order);
        variables.put("frontendUrl", frontendUrl);
        variables.put("supportEmail", "support@mosiacstore.com");
        variables.put("contactPhone", "+84 788-732-514");

        // Add t-shirt care instructions
        variables.put("careInstructions", getCareInstructions());

        return sendEmailWithTemplate(order.getUser().getEmail(), subject, template, variables);
    }

    /**
     * Send order cancelled email
     * @param order The cancelled order
     * @return CompletableFuture for async operation
     */
    @Async
    public CompletableFuture<Void> sendOrderCancelledEmail(Order order) {
        String subject = "Your Order #" + order.getOrderNumber() + " has been cancelled";
        String template = "order-cancelled";

        Map<String, Object> variables = new HashMap<>();
        variables.put("order", order);
        variables.put("frontendUrl", frontendUrl);
        variables.put("supportEmail", "support@mosiacstore.com");
        variables.put("contactPhone", "+84 788-732-514");

        return sendEmailWithTemplate(order.getUser().getEmail(), subject, template, variables);
    }

    /**
     * Helper method for sending emails with Thymeleaf templates with improved concurrency handling
     */
    private CompletableFuture<Void> sendEmailWithTemplate(
            String to, String subject, String templateName, Map<String, Object> variables) {

        // Use the concurrency manager to limit concurrent email operations
        // This helps with AWS t2.micro instances with limited resources
        return CompletableFuture.runAsync(() -> {
            concurrencyManager.executeWithConcurrencyControl("email", to, () -> {
                try {
                    MimeMessage mimeMessage = mailSender.createMimeMessage();
                    MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

                    helper.setFrom(senderEmail);
                    helper.setTo(to);
                    helper.setSubject(subject);

                    Context context = new Context(Locale.getDefault());
                    variables.forEach(context::setVariable);

                    String htmlContent = templateEngine.process("emails/" + templateName, context);
                    helper.setText(htmlContent, true);

                    mailSender.send(mimeMessage);
                    log.info("Email sent to {} with subject: {}", to, subject);

                } catch (MessagingException e) {
                    log.error("Failed to send email to {} with subject: {}", to, subject, e);
                    throw new RuntimeException("Email sending failed", e);
                }
            });
        });
    }

    /**
     * Send shipping fee notification email with QR code for payment
     * @param order The order
     * @param shippingFee The calculated shipping fee
     * @return CompletableFuture for async operation
     */
    @Async
    public CompletableFuture<Void> sendShippingFeeEmail(Order order, BigDecimal shippingFee) {
        String subject = "Phí vận chuyển cho đơn hàng #" + order.getOrderNumber();
        String template = "shipping-fee";

        Map<String, Object> variables = new HashMap<>();
        variables.put("order", order);
        variables.put("shippingFee", shippingFee);
        variables.put("bankName", "MOMO");
        variables.put("accountNumber", "0788732514");
        variables.put("transferDescription", "Ship DH" + order.getOrderNumber());
        variables.put("frontendUrl", frontendUrl);
        variables.put("supportEmail", "support@mosiacstore.com");
        variables.put("contactPhone", "+84 788-732-514");

        return sendEmailWithTemplate(order.getUser().getEmail(), subject, template, variables);
    }

    /**
     * Get t-shirt care instructions
     */
    private Map<String, String> getCareInstructions() {
        Map<String, String> instructions = new HashMap<>();
        instructions.put("washing", "Machine wash cold with similar colors. Use mild detergent. Do not bleach.");
        instructions.put("drying", "Tumble dry low or hang dry for best results to preserve the QR code quality.");
        instructions.put("ironing", "Iron inside out on low heat if needed. Avoid ironing directly over the QR code to ensure its functionality.");
        instructions.put("storage", "Store folded in a cool, dry place away from direct sunlight to preserve colors.");
        instructions.put("qrcode", "The QR code is designed to be durable, but avoid harsh scrubbing directly on the code area.");
        return instructions;
    }
}