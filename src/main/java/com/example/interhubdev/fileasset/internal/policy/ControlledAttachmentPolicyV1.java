package com.example.interhubdev.fileasset.internal.policy;

import com.example.interhubdev.fileasset.FileAssetArchiveProfile;
import com.example.interhubdev.fileasset.FileDeliveryProfile;
import com.example.interhubdev.fileasset.FilePolicyKey;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Set;

/**
 * Initial default policy used for the first fileasset migration step.
 */
@Component
class ControlledAttachmentPolicyV1 implements FileSecurityPolicy {

    @Override
    public FilePolicyKey key() {
        return FilePolicyKey.CONTROLLED_ATTACHMENT;
    }

    @Override
    public int version() {
        return 1;
    }

    @Override
    public Duration registeredTtl() {
        return Duration.ofHours(24);
    }

    @Override
    public Duration uploadedTtl() {
        return Duration.ofHours(24);
    }

    @Override
    public Duration activeUnboundTtl() {
        return Duration.ofDays(7);
    }

    @Override
    public int maxProcessingAttempts() {
        return 3;
    }

    @Override
    public long maxSizeBytes() {
        return 1024L * 1024L * 1024L;
    }

    @Override
    public Set<String> allowedDeclaredContentTypes() {
        return Set.of();
    }

    @Override
    public FileDeliveryProfile deliveryProfile() {
        return FileDeliveryProfile.CONTROLLED_ATTACHMENT_ONLY;
    }

    @Override
    public FileAssetArchiveProfile archiveProfile() {
        return FileAssetArchiveProfile.STANDARD;
    }
}
