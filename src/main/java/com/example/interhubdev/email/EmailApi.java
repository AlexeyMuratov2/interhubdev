package com.example.interhubdev.email;

import java.util.concurrent.CompletableFuture;

/**
 * Public API for the Email module.
 * Provides email sending capabilities for other modules.
 * 
 * <h2>Usage Examples</h2>
 * 
 * <h3>Simple text email:</h3>
 * <pre>{@code
 * emailApi.send(EmailMessage.text(
 *     "user@example.com",
 *     "Welcome!",
 *     "Hello, welcome to our platform."
 * ));
 * }</pre>
 * 
 * <h3>HTML email:</h3>
 * <pre>{@code
 * emailApi.send(EmailMessage.html(
 *     "user@example.com",
 *     "Welcome!",
 *     "<h1>Hello!</h1><p>Welcome to our platform.</p>"
 * ));
 * }</pre>
 * 
 * <h3>Templated email:</h3>
 * <pre>{@code
 * emailApi.send(EmailMessage.templated(
 *     "user@example.com",
 *     "Your Invitation",
 *     "invitation",
 *     Map.of("name", "John", "link", "https://...")
 * ));
 * }</pre>
 * 
 * <h3>Complex email with builder:</h3>
 * <pre>{@code
 * emailApi.send(EmailMessage.builder()
 *     .to("user@example.com")
 *     .subject("Important Update")
 *     .textBody("Plain text version")
 *     .htmlBody("<h1>HTML version</h1>")
 *     .cc("manager@example.com")
 *     .replyTo("support@example.com")
 *     .build()
 * );
 * }</pre>
 * 
 * <h3>Async sending:</h3>
 * <pre>{@code
 * emailApi.sendAsync(message)
 *     .thenAccept(result -> {
 *         if (result.success()) {
 *             log.info("Email sent: {}", result.messageId());
 *         } else {
 *             log.error("Failed: {}", result.error());
 *         }
 *     });
 * }</pre>
 */
public interface EmailApi {

    /**
     * Send an email synchronously.
     * Blocks until the email is sent or an error occurs.
     *
     * @param message the email message to send
     * @return result of the send operation
     */
    EmailResult send(EmailMessage message);

    /**
     * Send an email asynchronously.
     * Returns immediately with a future that completes when sending is done.
     *
     * @param message the email message to send
     * @return future that completes with the send result
     */
    CompletableFuture<EmailResult> sendAsync(EmailMessage message);

    /**
     * Check if the email service is properly configured and operational.
     * Useful for health checks.
     *
     * @return true if the service can send emails
     */
    boolean isOperational();

    /**
     * Get the configured sender (from) address.
     *
     * @return the default sender email address
     */
    String getDefaultSender();
}
