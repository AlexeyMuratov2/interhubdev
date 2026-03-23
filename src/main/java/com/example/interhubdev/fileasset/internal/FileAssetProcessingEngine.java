package com.example.interhubdev.fileasset.internal;

import com.example.interhubdev.error.AppException;
import com.example.interhubdev.fileasset.FileAssetArchiveProfile;
import com.example.interhubdev.fileasset.FileAssetSafetyClass;
import com.example.interhubdev.fileasset.FileDeliveryProfile;
import com.example.interhubdev.fileasset.internal.antivirus.FileAssetAntivirusPort;
import com.example.interhubdev.fileasset.internal.antivirus.FileAssetAntivirusPort.ScanVerdict;
import com.example.interhubdev.fileasset.internal.antivirus.ScanFailureReason;
import com.example.interhubdev.fileasset.internal.policy.AntivirusMode;
import com.example.interhubdev.fileasset.internal.policy.FileSecurityPolicy;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.YearMonth;

/**
 * Executes the internal technical processing pipeline for file assets.
 * <p>
 * The first implementation is intentionally minimal: it fixes the lifecycle hooks and policy
 * boundaries while keeping the actual scanning/classification logic easy to extend later.
 * </p>
 */
@Component
@RequiredArgsConstructor
class FileAssetProcessingEngine {

    private static final String HARDENED_CONTENT_TYPE = "application/octet-stream";
    private static final String HARDENED_CONTENT_DISPOSITION = "attachment";

    private final FileAssetStoragePort storagePort;
    private final FileAssetAntivirusPort antivirusPort;

    ProcessingResult process(FileAsset asset, FileSecurityPolicy policy) {
        if (asset.getTempObjectKey() == null || asset.getTempObjectKey().isBlank()) {
            throw FileAssetErrors.invalidTransition(asset.getId(), asset.getStatus(), "process without uploaded bytes");
        }
        if (!storagePort.exists(asset.getTempObjectKey())) {
            throw FileAssetErrors.uploadedObjectMissing(asset.getId());
        }

        runAntivirusIfRequired(asset, policy);

        String detectedContentType = normalizeContentType(asset.getDeclaredContentType());
        String finalObjectKey = asset.getFinalObjectKey();
        if (finalObjectKey == null || finalObjectKey.isBlank()) {
            finalObjectKey = buildFinalObjectKey(asset, policy);
        }

        FileAssetStoragePort.HardenedObjectMetadata hardenedMetadata = new FileAssetStoragePort.HardenedObjectMetadata(
            policy.forceBinaryObjectMetadata() ? HARDENED_CONTENT_TYPE : detectedContentType,
            HARDENED_CONTENT_DISPOSITION
        );
        storagePort.promoteToFinal(asset.getTempObjectKey(), finalObjectKey, asset.getSizeBytes(), hardenedMetadata);

        LocalDateTime nextExpiry = asset.getClaimedAt() == null
            ? LocalDateTime.now().plus(policy.activeUnboundTtl())
            : null;

        return new ProcessingResult(
            detectedContentType,
            finalObjectKey,
            policy.classify(asset),
            policy.deliveryProfile(),
            policy.archiveProfile(),
            nextExpiry
        );
    }

    private void runAntivirusIfRequired(FileAsset asset, FileSecurityPolicy policy) {
        if (policy.antivirusMode() != AntivirusMode.REQUIRED_FAIL_CLOSED) {
            return;
        }

        long maxScannableBytes = antivirusPort.capabilities().maxScannableBytes();
        if (asset.getSizeBytes() > maxScannableBytes) {
            throw FileAssetErrors.scannerCapacityExceeded(asset.getSizeBytes(), maxScannableBytes);
        }

        try (InputStream inputStream = storagePort.openStream(asset.getTempObjectKey())) {
            ScanVerdict verdict = antivirusPort.scan(
                inputStream,
                asset.getSizeBytes(),
                asset.getOriginalName(),
                asset.getDeclaredContentType()
            );
            if (verdict.status() == ScanVerdict.Status.CLEAN) {
                return;
            }
            if (verdict.status() == ScanVerdict.Status.INFECTED) {
                throw FileAssetErrors.malwareDetected(verdict.signatureName());
            }
            throw mapScanFailure(verdict.failureReason());
        } catch (AppException ex) {
            throw ex;
        } catch (Exception ex) {
            throw FileAssetErrors.scanFailed();
        }
    }

    private static AppException mapScanFailure(ScanFailureReason failureReason) {
        if (failureReason == null) {
            return FileAssetErrors.scanFailed();
        }
        return switch (failureReason) {
            case UNAVAILABLE -> FileAssetErrors.antivirusUnavailable();
            case TIMEOUT -> FileAssetErrors.antivirusTimeout();
            case SIZE_LIMIT_EXCEEDED -> FileAssetErrors.scanFailed();
            case SCAN_ERROR -> FileAssetErrors.scanFailed();
        };
    }

    private static String buildFinalObjectKey(FileAsset asset, FileSecurityPolicy policy) {
        if (policy.opaqueObjectKey()) {
            YearMonth yearMonth = YearMonth.now();
            return "fileassets/final/" + yearMonth.getYear() + "/" + yearMonth.getMonthValue() + "/"
                + asset.getId();
        }
        String sanitizedName = sanitizeFilename(asset.getOriginalName());
        YearMonth yearMonth = YearMonth.now();
        return "fileassets/final/" + yearMonth.getYear() + "/" + yearMonth.getMonthValue() + "/"
            + asset.getId() + "_" + sanitizedName;
    }

    private static String normalizeContentType(String declaredContentType) {
        if (declaredContentType == null || declaredContentType.isBlank()) {
            return "application/octet-stream";
        }
        return declaredContentType.split(";")[0].trim().toLowerCase();
    }

    private static String sanitizeFilename(String name) {
        if (name == null || name.isBlank()) {
            return "file";
        }
        String sanitized = name.replace('\\', '_').replace('/', '_').replace("\0", "");
        if (sanitized.length() > 255) {
            sanitized = sanitized.substring(0, 255);
        }
        return sanitized.isEmpty() ? "file" : sanitized;
    }

    record ProcessingResult(
        String detectedContentType,
        String finalObjectKey,
        FileAssetSafetyClass safetyClass,
        FileDeliveryProfile deliveryProfile,
        FileAssetArchiveProfile archiveProfile,
        LocalDateTime nextExpiresAt
    ) {
    }
}
