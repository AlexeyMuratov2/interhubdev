package com.example.interhubdev.invitation;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Public API for the Invitation module.
 * Handles user invitations and account activation flow.
 */
public interface InvitationApi {

    // ==================== Query methods ====================

    /**
     * Find invitation by ID.
     */
    Optional<InvitationDto> findById(UUID id);

    /**
     * Find invitation by user ID.
     */
    Optional<InvitationDto> findByUserId(UUID userId);

    /**
     * Get invitations with cursor-based pagination.
     * Sorted by send time (createdAt) descending — newest first.
     * Max 30 items per page.
     *
     * @param cursor optional cursor (last invitation id from previous page); null for first page
     * @param limit  page size, 1–30 (default 30)
     * @return page with items and optional next cursor
     */
    InvitationPage findPage(UUID cursor, int limit);

    /**
     * Get invitations by status.
     */
    List<InvitationDto> findByStatus(InvitationStatus status);

    /**
     * Get invitations created by a specific admin.
     */
    List<InvitationDto> findByInvitedBy(UUID adminId);

    // ==================== Command methods ====================

    /**
     * Create a new invitation.
     * Creates user with PENDING status and sends invitation email.
     *
     * @param request    invitation data including user and role-specific profile
     * @param invitedBy  ID of the admin creating the invitation
     * @return created invitation
     * @throws IllegalArgumentException if email already exists or inviter cannot invite this role
     */
    InvitationDto create(CreateInvitationRequest request, UUID invitedBy);

    /**
     * Resend invitation email.
     * Generates new token and sends email.
     *
     * @param invitationId invitation ID
     * @throws IllegalArgumentException if invitation not found
     * @throws IllegalStateException    if invitation cannot be resent (expired, cancelled, accepted)
     */
    void resend(UUID invitationId);

    /**
     * Cancel an invitation.
     * User will not be able to activate their account.
     *
     * @param invitationId invitation ID
     * @throws IllegalArgumentException if invitation not found
     * @throws IllegalStateException    if invitation already accepted
     */
    void cancel(UUID invitationId);

    /**
     * Delete invitation linked to the given user (e.g. when user account is deleted).
     * No-op if no invitation exists for the user.
     *
     * @param userId user ID
     */
    void deleteByUserId(UUID userId);

    // ==================== Token validation ====================

    /**
     * Validate invitation token.
     * If token is expired but invitation is valid, generates new token and sends email.
     *
     * @param token the invitation token from email link
     * @return validation result with invitation details or error
     */
    TokenValidationResult validateToken(String token);

    /**
     * Accept invitation and activate user account.
     *
     * @param request token and password
     * @throws IllegalArgumentException if token invalid or expired
     * @throws IllegalStateException    if invitation already accepted
     */
    void accept(AcceptInvitationRequest request);
}
