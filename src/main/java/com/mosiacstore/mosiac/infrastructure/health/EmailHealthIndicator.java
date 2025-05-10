package com.mosiacstore.mosiac.infrastructure.health;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.stereotype.Component;

import java.util.Properties;

/**
 * Health indicator for the email system
 * Used by Spring Boot Actuator to report on email service health
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class EmailHealthIndicator implements HealthIndicator {

    private final JavaMailSender mailSender;

    @Override
    public Health health() {
        try {
            // Extract mail properties for diagnostics
            JavaMailSenderImpl mailSenderImpl = (JavaMailSenderImpl) mailSender;
            String host = mailSenderImpl.getHost();
            int port = mailSenderImpl.getPort();
            String username = mailSenderImpl.getUsername();
            Properties properties = mailSenderImpl.getJavaMailProperties();

            // Test mail server connectivity (without actually sending an email)
            mailSenderImpl.testConnection();

            // If the test passes, return up with details
            return Health.up()
                    .withDetail("host", host)
                    .withDetail("port", port)
                    .withDetail("username", username != null ? username : "not set")
                    .withDetail("protocol", properties.getProperty("mail.transport.protocol", "unknown"))
                    .withDetail("auth", properties.getProperty("mail.smtp.auth", "unknown"))
                    .withDetail("starttls", properties.getProperty("mail.smtp.starttls.enable", "unknown"))
                    .build();

        } catch (Exception e) {
            // If any exception occurs, return down with error details
            log.error("Email service health check failed", e);
            return Health.down()
                    .withDetail("error", e.getMessage())
                    .withDetail("exception", e.getClass().getName())
                    .build();
        }
    }
}