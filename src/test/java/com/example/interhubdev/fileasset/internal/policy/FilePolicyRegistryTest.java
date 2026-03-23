package com.example.interhubdev.fileasset.internal.policy;

import com.example.interhubdev.error.AppException;
import com.example.interhubdev.fileasset.FileAssetArchiveProfile;
import com.example.interhubdev.fileasset.FileAssetSafetyClass;
import com.example.interhubdev.fileasset.FileDeliveryProfile;
import com.example.interhubdev.fileasset.FilePolicyKey;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("FilePolicyRegistry")
class FilePolicyRegistryTest {

    @Test
    @DisplayName("resolves highest version as current policy")
    void resolvesCurrentPolicy() {
        FilePolicyRegistry registry = new FilePolicyRegistry(List.of(
            new TestPolicy(FilePolicyKey.CONTROLLED_ATTACHMENT, 1),
            new TestPolicy(FilePolicyKey.CONTROLLED_ATTACHMENT, 2)
        ));

        FileSecurityPolicy current = registry.resolveCurrent(FilePolicyKey.CONTROLLED_ATTACHMENT);

        assertThat(current.version()).isEqualTo(2);
    }

    @Test
    @DisplayName("resolves exact pinned version")
    void resolvesExactVersion() {
        FilePolicyRegistry registry = new FilePolicyRegistry(List.of(
            new TestPolicy(FilePolicyKey.CONTROLLED_ATTACHMENT, 1),
            new TestPolicy(FilePolicyKey.CONTROLLED_ATTACHMENT, 2)
        ));

        FileSecurityPolicy current = registry.resolveExact(FilePolicyKey.CONTROLLED_ATTACHMENT, 1);

        assertThat(current.version()).isEqualTo(1);
    }

    @Test
    @DisplayName("throws when current policy is missing")
    void missingCurrentPolicy() {
        FilePolicyRegistry registry = new FilePolicyRegistry(List.of());

        assertThatThrownBy(() -> registry.resolveCurrent(FilePolicyKey.CONTROLLED_ATTACHMENT))
            .isInstanceOf(AppException.class)
            .hasMessageContaining("Unsupported file policy");
    }

    private record TestPolicy(FilePolicyKey key, int version) implements FileSecurityPolicy {

        @Override
        public Duration registeredTtl() {
            return Duration.ofHours(1);
        }

        @Override
        public Duration uploadedTtl() {
            return Duration.ofHours(1);
        }

        @Override
        public Duration activeUnboundTtl() {
            return Duration.ofHours(1);
        }

        @Override
        public int maxProcessingAttempts() {
            return 3;
        }

        @Override
        public long maxSizeBytes() {
            return 100;
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

        @Override
        public FileAssetSafetyClass classify(com.example.interhubdev.fileasset.internal.FileAsset asset) {
            return FileAssetSafetyClass.CONTROLLED_ATTACHMENT_ONLY;
        }
    }
}
