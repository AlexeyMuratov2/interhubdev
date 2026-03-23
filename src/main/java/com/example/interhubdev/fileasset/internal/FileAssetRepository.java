package com.example.interhubdev.fileasset.internal;

import com.example.interhubdev.fileasset.FileAssetStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for file_asset lifecycle persistence.
 */
@Repository
interface FileAssetRepository extends JpaRepository<FileAsset, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select fa from FileAsset fa where fa.id = :id")
    Optional<FileAsset> findByIdForUpdate(@Param("id") UUID id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        select fa from FileAsset fa
        where fa.status in :statuses
          and fa.expiresAt is not null
          and fa.expiresAt <= :now
          and (fa.status <> com.example.interhubdev.fileasset.FileAssetStatus.ACTIVE or fa.claimedAt is null)
        order by fa.expiresAt asc
        """)
    List<FileAsset> findExpirableAssetsForUpdate(
        @Param("statuses") Collection<FileAssetStatus> statuses,
        @Param("now") LocalDateTime now
    );
}
