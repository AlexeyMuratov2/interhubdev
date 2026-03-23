package com.example.interhubdev.fileasset.internal;

import com.example.interhubdev.fileasset.FileAssetView;

/**
 * Entity to safe metadata projection mapping.
 */
final class FileAssetMapper {

    private FileAssetMapper() {
    }

    static FileAssetView toView(FileAsset entity) {
        return new FileAssetView(
            entity.getId(),
            entity.getPolicyKey(),
            entity.getPolicyVersion(),
            entity.getStatus(),
            entity.getOriginalName(),
            entity.getDeclaredContentType(),
            entity.getDetectedContentType(),
            entity.getSizeBytes(),
            entity.getChecksum(),
            entity.getUploadedBy(),
            entity.getSafetyClass(),
            entity.getDeliveryProfile(),
            entity.getArchiveProfile(),
            entity.getProcessingAttempts(),
            entity.getLastFailureCode(),
            entity.getLastFailureMessage(),
            entity.getExpiresAt(),
            entity.getClaimedAt(),
            entity.getCreatedAt(),
            entity.getUploadedAt(),
            entity.getActivatedAt(),
            entity.getFailedAt(),
            entity.getDeletedAt()
        );
    }
}
