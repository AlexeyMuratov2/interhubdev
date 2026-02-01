package com.example.interhubdev.email;

import java.time.Instant;

/**
 * Result of an email send operation.
 *
 * @param success   true if email was sent successfully
 * @param messageId unique identifier for the sent message (null if failed)
 * @param recipient the email address the message was sent to
 * @param sentAt    timestamp when the email was sent (null if failed)
 * @param error     error message if sending failed (null if success)
 */
public record EmailResult(
    boolean success,
    String messageId,
    String recipient,
    Instant sentAt,
    String error
) {
    /**
     * Creates a successful result.
     */
    public static EmailResult success(String messageId, String recipient) {
        return new EmailResult(true, messageId, recipient, Instant.now(), null);
    }

    /**
     * Creates a failed result.
     */
    public static EmailResult failure(String recipient, String error) {
        return new EmailResult(false, null, recipient, null, error);
    }

    /**
     * Creates a failed result from an exception.
     */
    public static EmailResult failure(String recipient, Throwable exception) {
        return failure(recipient, exception.getMessage());
    }
}
