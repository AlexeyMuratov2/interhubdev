package com.example.interhubdev.invitation.internal;

import com.example.interhubdev.invitation.InvitationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for Invitation entity.
 */
interface InvitationRepository extends JpaRepository<Invitation, UUID> {

    Optional<Invitation> findByUserId(UUID userId);

    List<Invitation> findByStatus(InvitationStatus status);

    List<Invitation> findByInvitedById(UUID invitedById);

    boolean existsByUserId(UUID userId);
}
