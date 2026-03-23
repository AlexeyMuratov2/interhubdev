package com.example.interhubdev.fileasset.internal.policy;

import com.example.interhubdev.fileasset.FileAssetArchiveProfile;
import com.example.interhubdev.fileasset.FileDeliveryProfile;
import com.example.interhubdev.fileasset.FilePolicyKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Set;

/**
 * Hardened policy for server-stored files that must never be executed or previewed inline.
 */
@Component
class ControlledAttachmentPolicyV2 implements FileSecurityPolicy {

    private final long maxSizeBytes;

    ControlledAttachmentPolicyV2(
        @Value("${app.fileasset.controlled-attachment.max-size-bytes:3221225472}") long maxSizeBytes
    ) {
        this.maxSizeBytes = maxSizeBytes;
    }

    @Override
    public FilePolicyKey key() {
        return FilePolicyKey.CONTROLLED_ATTACHMENT;
    }

    @Override
    public int version() {
        return 2;
    }

    @Override
    public Duration registeredTtl() {
        return Duration.ofHours(48);
    }

    @Override
    public Duration uploadedTtl() {
        return Duration.ofHours(24);
    }

    @Override
    public Duration activeUnboundTtl() {
        return Duration.ofDays(10);
    }

    @Override
    public int maxProcessingAttempts() {
        return 3;
    }

    @Override
    public long maxSizeBytes() {
        return maxSizeBytes;
    }

    @Override
    public Set<String> allowedDeclaredContentTypes() {
        return Set.of();
    }

    @Override
    public FileDeliveryProfile deliveryProfile() {
        return FileDeliveryProfile.BACKEND_ATTACHMENT_STREAM_ONLY;
    }

    @Override
    public FileAssetArchiveProfile archiveProfile() {
        return FileAssetArchiveProfile.OPAQUE_NO_SERVER_EXTRACTION;
    }

    @Override
    public AntivirusMode antivirusMode() {
        return AntivirusMode.REQUIRED_FAIL_CLOSED;
    }

    @Override
    public ArchiveHandlingMode archiveHandlingMode() {
        return ArchiveHandlingMode.OPAQUE_NO_SERVER_EXTRACTION;
    }

    @Override
    public ExecutionIsolationProfile executionIsolationProfile() {
        return ExecutionIsolationProfile.NEVER_EXECUTE_SERVER_SIDE;
    }

    @Override
    public boolean forceBinaryObjectMetadata() {
        return true;
    }

    @Override
    public boolean opaqueObjectKey() {
        return true;
    }
}
