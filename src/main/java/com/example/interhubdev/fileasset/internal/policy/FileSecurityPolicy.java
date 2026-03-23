package com.example.interhubdev.fileasset.internal.policy;

import com.example.interhubdev.fileasset.FileAssetArchiveProfile;
import com.example.interhubdev.fileasset.FileAssetSafetyClass;
import com.example.interhubdev.fileasset.FileDeliveryProfile;
import com.example.interhubdev.fileasset.FilePolicyKey;
import com.example.interhubdev.fileasset.internal.FileAsset;
import com.example.interhubdev.fileasset.internal.FileAssetErrors;

import java.time.Duration;
import java.util.Set;

/**
 * Internal definition of a file security and processing policy.
 */
public interface FileSecurityPolicy {

    FilePolicyKey key();

    int version();

    Duration registeredTtl();

    Duration uploadedTtl();

    Duration activeUnboundTtl();

    int maxProcessingAttempts();

    long maxSizeBytes();

    Set<String> allowedDeclaredContentTypes();

    FileDeliveryProfile deliveryProfile();

    FileAssetArchiveProfile archiveProfile();

    default AntivirusMode antivirusMode() {
        return AntivirusMode.DISABLED;
    }

    default ArchiveHandlingMode archiveHandlingMode() {
        return ArchiveHandlingMode.STANDARD;
    }

    default ExecutionIsolationProfile executionIsolationProfile() {
        return ExecutionIsolationProfile.BASIC_STORAGE_ISOLATION;
    }

    default boolean forceBinaryObjectMetadata() {
        return false;
    }

    default boolean opaqueObjectKey() {
        return false;
    }

    default FileAssetSafetyClass classify(FileAsset asset) {
        return FileAssetSafetyClass.CONTROLLED_ATTACHMENT_ONLY;
    }

    default void validateRegistration(String originalName, String declaredContentType, long sizeBytes) {
        if (originalName == null || originalName.isBlank()) {
            throw FileAssetErrors.invalidRegistration("originalName is required");
        }
        if (originalName.length() > 512) {
            throw FileAssetErrors.invalidRegistration("originalName must not exceed 512 characters");
        }
        if (sizeBytes <= 0) {
            throw FileAssetErrors.invalidRegistration("sizeBytes must be positive");
        }
        if (sizeBytes > maxSizeBytes()) {
            throw FileAssetErrors.invalidRegistration("sizeBytes exceeds policy limit");
        }
        if (declaredContentType == null || declaredContentType.isBlank()) {
            return;
        }
        Set<String> allowed = allowedDeclaredContentTypes();
        if (!allowed.isEmpty()) {
            String normalized = declaredContentType.split(";")[0].trim().toLowerCase();
            if (!allowed.contains(normalized)) {
                throw FileAssetErrors.invalidRegistration("declaredContentType is not allowed by policy");
            }
        }
    }
}
