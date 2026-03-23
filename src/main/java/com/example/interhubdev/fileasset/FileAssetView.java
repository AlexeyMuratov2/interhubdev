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
}
