package com.example.interhubdev.auth.internal;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for refresh token operations.
 */
interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

    /**
     * Find token by its hash.
     */
    Optional<RefreshToken> findByTokenHash(String tokenHash);

    /**
     * Find all active (non-revoked) tokens for a user.
     */
    List<RefreshToken> findByUserIdAndRevokedFalse(UUID userId);

    /**
     * Revoke all tokens for a user (logout from all devices).
     */
    @Modifying
    @Query("UPDATE RefreshToken t SET t.revoked = true, t.revokedAt = :now WHERE t.userId = :userId AND t.revoked = false")
    int revokeAllByUserId(@Param("userId") UUID userId, @Param("now") LocalDateTime now);

    /**
     * Delete expired and revoked tokens (cleanup job).
     */
    @Modifying
    @Query("DELETE FROM RefreshToken t WHERE t.expiresAt < :cutoff OR (t.revoked = true AND t.revokedAt < :cutoff)")
    int deleteExpiredAndRevoked(@Param("cutoff") LocalDateTime cutoff);

    /**
     * Count active sessions for a user.
     */
    long countByUserIdAndRevokedFalseAndExpiresAtAfter(UUID userId, LocalDateTime now);
}
