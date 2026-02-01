package com.example.interhubdev.invitation.internal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
 * Invitation token entity.
 * Short-lived token (24 hours) sent in email links.
 * Can be regenerated if expired but invitation is still valid.
 */
@Entity
@Table(name = "invitation_tokens")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
class InvitationToken {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /**
     * Reference to the invitation.
     */
    @Column(name = "invitation_id", nullable = false)
    private UUID invitationId;

    /**
     * The actual token string (UUID).
     */
    @Column(nullable = false, unique = true)
    private String token;

    /**
     * When the token expires (24 hours from creation).
     */
    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();

    /**
     * Check if token has expired.
     */
    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }

    /**
     * Check if token is valid (not expired).
     */
    public boolean isValid() {
        return !isExpired();
    }

    /**
     * Generate a new random token string.
     */
    public static String generateTokenString() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}
