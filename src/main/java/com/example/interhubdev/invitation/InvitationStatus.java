package com.example.interhubdev.invitation;

/**
 * Status of an invitation.
 */
public enum InvitationStatus {
    /**
     * Invitation created, email not yet sent.
     */
    PENDING,

    /**
     * Email is being sent (in progress).
     */
    SENDING,

    /**
     * Email successfully sent, waiting for user to accept.
     */
    SENT,

    /**
     * Failed to send email after all retry attempts.
     */
    FAILED,

    /**
     * User accepted the invitation and activated their account.
     */
    ACCEPTED,

    /**
     * Invitation expired (exceeded validity period).
     */
    EXPIRED,

    /**
     * Invitation was cancelled by an administrator.
     */
    CANCELLED
}
