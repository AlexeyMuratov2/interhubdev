package com.example.interhubdev.invitation.internal;

import com.example.interhubdev.invitation.InvitationStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
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

    /** First page: order by send time (createdAt) desc, id desc. Max 31 to detect hasNext. */
    List<Invitation> findFirst31ByOrderByCreatedAtDescIdDesc();

    /** Next page: invitations after cursor — (createdAt < :cursorCreatedAt) OR (createdAt = :cursorCreatedAt AND id < :cursorId). Limit via Pageable (e.g. 31). */
    @Query("SELECT i FROM Invitation i WHERE (i.createdAt < :cursorCreatedAt) OR (i.createdAt = :cursorCreatedAt AND i.id < :cursorId) ORDER BY i.createdAt DESC, i.id DESC")
    List<Invitation> findAfterCursor(@Param("cursorCreatedAt") Instant cursorCreatedAt, @Param("cursorId") UUID cursorId, Pageable pageable);
}
