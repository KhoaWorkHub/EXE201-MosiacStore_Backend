package com.mosiacstore.mosiac.application.service.Impl;

import com.mosiacstore.mosiac.domain.order.Order;
import com.mosiacstore.mosiac.infrastructure.service.ConcurrencyManager;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

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
     * Send order confirmation email
     * @param order The order to confirm
     * @return CompletableFuture for async operation
     */
    @Async
    public CompletableFuture<Void> sendOrderConfirmationEmail(Order order) {
        String subject = "Your Order #" + order.getOrderNumber() + " has been received";
        String template = "order-confirmation";

        Map<String, Object> variables = new HashMap<>();
        variables.put("order", order);
        variables.put("frontendUrl", frontendUrl);
        variables.put("supportEmail", "support@mosiacstore.com");
        variables.put("contactPhone", "+84 788-732-514");

        return sendEmailWithTemplate(order.getUser().getEmail(), subject, template, variables);
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