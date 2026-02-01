package com.example.interhubdev.invitation.internal;

import com.example.interhubdev.invitation.InvitationStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * Invitation entity.
 * Represents an invitation sent to a user to join the system.
 * Validity period: 3 months from creation.
 */
@Entity
@Table(name = "invitations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
class Invitation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /**
     * Reference to the user being invited.
     */
    @Column(name = "user_id", nullable = false)
    private UUID userId;

    /**
     * Reference to the admin who created the invitation.
     */
    @Column(name = "invited_by_id")
    private UUID invitedById;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private InvitationStatus status = InvitationStatus.PENDING;

    /**
     * When the invitation email was last sent.
     */
    @Column(name = "email_sent_at")
    private Instant emailSentAt;

    /**
     * Message ID from email service (for tracking).
     */
    @Column(name = "email_message_id")
    private String emailMessageId;

    /**
     * Number of email send attempts.
     */
    @Column(name = "email_attempts")
    @Builder.Default
    private int emailAttempts = 0;

    /**
     * When the invitation expires (3 months from creation).
     */
    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    /**
     * When the user accepted the invitation.
     */
    @Column(name = "accepted_at")
    private Instant acceptedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    @Builder.Default
    private Instant updatedAt = Instant.now();

    /**
     * Check if invitation has expired.
     */
    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }

    /**
     * Check if invitation can be accepted.
     */
    public boolean canBeAccepted() {
        return !isExpired() 
            && status != InvitationStatus.ACCEPTED 
            && status != InvitationStatus.CANCELLED
            && status != InvitationStatus.EXPIRED;
    }

    /**
     * Increment email attempts counter.
     */
    public void incrementEmailAttempts() {
        this.emailAttempts++;
        this.updatedAt = Instant.now();
    }

    /**
     * Mark email as sent.
     */
    public void markEmailSent(String messageId) {
        this.status = InvitationStatus.SENT;
        this.emailSentAt = Instant.now();
        this.emailMessageId = messageId;
        this.updatedAt = Instant.now();
    }

    /**
     * Mark as accepted.
     */
    public void markAccepted() {
        this.status = InvitationStatus.ACCEPTED;
        this.acceptedAt = Instant.now();
        this.updatedAt = Instant.now();
    }
}
