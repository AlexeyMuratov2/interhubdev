package com.example.interhubdev.invitation.internal;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository for InvitationToken entity.
 */
interface InvitationTokenRepository extends JpaRepository<InvitationToken, UUID> {

    Optional<InvitationToken> findByToken(String token);

    Optional<InvitationToken> findByInvitationId(UUID invitationId);

    boolean existsByToken(String token);

    /**
     * Delete all tokens for an invitation.
     */
    @Modifying
    @Query("DELETE FROM InvitationToken t WHERE t.invitationId = :invitationId")
    void deleteByInvitationId(UUID invitationId);
}
