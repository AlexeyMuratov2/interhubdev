package com.example.interhubdev.email.internal;

import com.example.interhubdev.email.EmailApi;
import com.example.interhubdev.email.EmailMessage;
import com.example.interhubdev.email.EmailResult;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Implementation of the Email API.
 * Handles actual email sending via Spring Mail / JavaMailSender.
 */
@Service
@RequiredArgsConstructor
@EnableConfigurationProperties(EmailProperties.class)
@Slf4j
class EmailServiceImpl implements EmailApi {

    private final JavaMailSender mailSender;
    private final EmailProperties properties;

    @Override
    public EmailResult send(EmailMessage message) {
        if (!properties.isEnabled()) {
            log.warn("Email sending is disabled. Would have sent to: {}", message.to());
            return EmailResult.success(generateMessageId(), message.to());
        }

        if (properties.isLogOnly()) {
            logEmail(message);
            return EmailResult.success(generateMessageId(), message.to());
        }

        try {
            String messageId = doSend(message);
            log.info("Email sent successfully to: {} [{}]", message.to(), messageId);
            return EmailResult.success(messageId, message.to());
        } catch (Exception e) {
            log.error("Failed to send email to {}: {}", message.to(), e.getMessage(), e);
            return EmailResult.failure(message.to(), e);
        }
    }

    @Override
    @Async
    public CompletableFuture<EmailResult> sendAsync(EmailMessage message) {
        return CompletableFuture.completedFuture(send(message));
    }

    @Override
    public boolean isOperational() {
        if (!properties.isEnabled()) {
            return true; // Disabled is still "operational" (just not sending)
        }
        
        try {
            // Test connection by getting a message object
            mailSender.createMimeMessage();
            return true;
        } catch (Exception e) {
            log.warn("Email service is not operational: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public String getDefaultSender() {
        return properties.getFrom();
    }

    /**
     * Actually send the email via JavaMailSender.
     */
    private String doSend(EmailMessage message) throws MessagingException {
        MimeMessage mimeMessage = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

        // Set from
        if (properties.getFromName() != null && !properties.getFromName().isBlank()) {
            try {
                helper.setFrom(properties.getFrom(), properties.getFromName());
            } catch (Exception e) {
                helper.setFrom(properties.getFrom());
            }
        } else {
            helper.setFrom(properties.getFrom());
        }

        // Set to
        helper.setTo(message.to());

        // Set subject
        helper.setSubject(message.subject());

        // Set body (text and/or HTML)
        if (message.hasHtml()) {
            if (message.textBody() != null && !message.textBody().isBlank()) {
                // Both text and HTML - multipart/alternative
                helper.setText(message.textBody(), message.htmlBody());
            } else {
                // HTML only
                helper.setText(message.htmlBody(), true);
            }
        } else if (message.isTemplated()) {
            // Template will be processed here (for now, just use template name as placeholder)
            String content = processTemplate(message);
            helper.setText(content, true);
        } else {
            // Text only
            helper.setText(message.textBody(), false);
        }

        // Set CC
        if (message.cc() != null && !message.cc().isEmpty()) {
            helper.setCc(message.cc().toArray(new String[0]));
        }

        // Set BCC
        if (message.bcc() != null && !message.bcc().isEmpty()) {
            helper.setBcc(message.bcc().toArray(new String[0]));
        }

        // Set reply-to
        if (message.replyTo() != null && !message.replyTo().isBlank()) {
            helper.setReplyTo(message.replyTo());
        }

        // Send
        mailSender.send(mimeMessage);

        return generateMessageId();
    }

    /**
     * Process email template.
     * TODO: Implement proper template engine (Thymeleaf, FreeMarker, etc.)
     */
    private String processTemplate(EmailMessage message) {
        // Basic template processing - replace variables in a simple format
        // For now, just return a placeholder. Real implementation would use Thymeleaf/FreeMarker.
        StringBuilder sb = new StringBuilder();
        sb.append("<html><body>");
        sb.append("<p>Template: ").append(message.templateName()).append("</p>");
        
        if (message.templateVars() != null && !message.templateVars().isEmpty()) {
            sb.append("<ul>");
            message.templateVars().forEach((key, value) -> 
                sb.append("<li>").append(key).append(": ").append(value).append("</li>")
            );
            sb.append("</ul>");
        }
        
        sb.append("</body></html>");
        return sb.toString();
    }

    /**
     * Log email for development/debugging.
     */
    private void logEmail(EmailMessage message) {
        log.info("""
            ========== EMAIL (LOG ONLY MODE) ==========
            To: {}
            Subject: {}
            Text Body: {}
            HTML Body: {}
            Template: {}
            Template Vars: {}
            ===========================================
            """,
            message.to(),
            message.subject(),
            message.textBody(),
            message.htmlBody() != null ? "[HTML content]" : null,
            message.templateName(),
            message.templateVars()
        );
    }

    private String generateMessageId() {
        return UUID.randomUUID().toString();
    }
}
