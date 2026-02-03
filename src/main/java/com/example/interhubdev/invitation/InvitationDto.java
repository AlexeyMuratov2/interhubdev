package com.example.interhubdev.invitation;

import com.example.interhubdev.user.Role;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Data Transfer Object for Invitation.
 */
public record InvitationDto(
    UUID id,
    UUID userId,
    String email,
    List<Role> roles,
    String firstName,
    String lastName,
    InvitationStatus status,
    UUID invitedById,
    Instant emailSentAt,
    int emailAttempts,
    Instant expiresAt,
    Instant acceptedAt,
    Instant createdAt
) {
    /**
     * Check if invitation is still valid (not expired, cancelled, or accepted).
     */
    public boolean isValid() {
        return status != InvitationStatus.EXPIRED 
            && status != InvitationStatus.CANCELLED 
            && status != InvitationStatus.ACCEPTED
            && Instant.now().isBefore(expiresAt);
    }

    /**
     * Check if invitation can be resent.
     */
    public boolean canResend() {
        return isValid() && (status == InvitationStatus.SENT || status == InvitationStatus.FAILED);
    }
}
