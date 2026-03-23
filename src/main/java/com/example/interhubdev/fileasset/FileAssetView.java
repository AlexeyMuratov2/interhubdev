package com.example.interhubdev.fileasset;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Safe metadata projection of a file asset.
 * <p>
 * Physical storage coordinates are intentionally not exposed.
 */
public record FileAssetView(
    UUID id,
    FilePolicyKey policyKey,
    int policyVersion,
    FileAssetStatus status,
    String originalName,
    String declaredContentType,
    String detectedContentType,
    long sizeBytes,
    String checksum,
    UUID uploadedBy,
    FileAssetSafetyClass safetyClass,
    FileDeliveryProfile deliveryProfile,
    FileAssetArchiveProfile archiveProfile,
    int processingAttempts,
    String lastFailureCode,
    String lastFailureMessage,
    LocalDateTime expiresAt,
    LocalDateTime claimedAt,
    LocalDateTime createdAt,
    LocalDateTime uploadedAt,
    LocalDateTime activatedAt,
    LocalDateTime failedAt,
    LocalDateTime deletedAt
) {

    public FileAssetStage stage() {
        return switch (status) {
            case REGISTERED, UPLOADED -> FileAssetStage.RECEIVED;
            case PROCESSING -> FileAssetStage.SCANNING;
            case ACTIVE -> FileAssetStage.READY;
            case FAILED, DELETED, EXPIRED -> FileAssetStage.FAILED;
        };
    }

    public int progressPercent() {
        return switch (status) {
            case REGISTERED -> 10;
            case UPLOADED -> 25;
            case PROCESSING -> 60;
            case ACTIVE, FAILED, DELETED, EXPIRED -> 100;
        };
    }

    public boolean downloadAvailable() {
        return status == FileAssetStatus.ACTIVE;
    }
}
