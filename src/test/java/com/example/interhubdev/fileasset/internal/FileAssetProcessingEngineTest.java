package com.example.interhubdev.fileasset.internal;

import com.example.interhubdev.error.AppException;
import com.example.interhubdev.fileasset.FileAssetArchiveProfile;
import com.example.interhubdev.fileasset.FileAssetSafetyClass;
import com.example.interhubdev.fileasset.FileAssetStatus;
import com.example.interhubdev.fileasset.FileDeliveryProfile;
import com.example.interhubdev.fileasset.FilePolicyKey;
import com.example.interhubdev.fileasset.internal.antivirus.AntivirusCapabilities;
import com.example.interhubdev.fileasset.internal.antivirus.FileAssetAntivirusPort;
import com.example.interhubdev.fileasset.internal.antivirus.ScanFailureReason;
import com.example.interhubdev.fileasset.internal.policy.ArchiveHandlingMode;
import com.example.interhubdev.fileasset.internal.policy.AntivirusMode;
import com.example.interhubdev.fileasset.internal.policy.ExecutionIsolationProfile;
import com.example.interhubdev.fileasset.internal.policy.FileSecurityPolicy;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("FileAssetProcessingEngine")
class FileAssetProcessingEngineTest {

    @Mock
    private FileAssetStoragePort storagePort;

    @Mock
    private FileAssetAntivirusPort antivirusPort;

    @Test
    @DisplayName("builds opaque final key and hardened delivery result for v2 policy")
    void buildsOpaqueFinalKeyForV2() {
        FileAssetProcessingEngine engine = new FileAssetProcessingEngine(storagePort, antivirusPort);
        FileAsset asset = uploadedAsset(120);
        FileSecurityPolicy policy = new V2Policy();

        when(storagePort.exists(asset.getTempObjectKey())).thenReturn(true);
        when(storagePort.openStream(asset.getTempObjectKey())).thenReturn(new ByteArrayInputStream("data".getBytes()));
        when(antivirusPort.capabilities()).thenReturn(new AntivirusCapabilities(10_000));
        when(antivirusPort.scan(any(), eq(asset.getSizeBytes()), eq(asset.getOriginalName()), eq(asset.getDeclaredContentType())))
            .thenReturn(FileAssetAntivirusPort.ScanVerdict.clean());

        FileAssetProcessingEngine.ProcessingResult result = engine.process(asset, policy);

        assertThat(result.finalObjectKey()).endsWith(asset.getId().toString());
        assertThat(result.finalObjectKey()).doesNotContain("report.pdf").doesNotContain("_");
        assertThat(result.deliveryProfile()).isEqualTo(FileDeliveryProfile.BACKEND_ATTACHMENT_STREAM_ONLY);
        assertThat(result.archiveProfile()).isEqualTo(FileAssetArchiveProfile.OPAQUE_NO_SERVER_EXTRACTION);
        verify(storagePort).promoteToFinal(
            eq(asset.getTempObjectKey()),
            eq(result.finalObjectKey()),
            eq(asset.getSizeBytes()),
            eq(new FileAssetStoragePort.HardenedObjectMetadata("application/octet-stream", "attachment"))
        );
    }

    @Test
    @DisplayName("fails closed when antivirus capacity is below file size")
    void failsClosedWhenScannerCapacityTooLow() {
        FileAssetProcessingEngine engine = new FileAssetProcessingEngine(storagePort, antivirusPort);
        FileAsset asset = uploadedAsset(3_000);

        when(storagePort.exists(asset.getTempObjectKey())).thenReturn(true);
        when(antivirusPort.capabilities()).thenReturn(new AntivirusCapabilities(2_000));

        assertThatThrownBy(() -> engine.process(asset, new V2Policy()))
            .isInstanceOf(AppException.class)
            .hasMessageContaining("Antivirus can scan up to");
    }

    @Test
    @DisplayName("fails closed when antivirus finds malware")
    void failsClosedWhenMalwareDetected() {
        FileAssetProcessingEngine engine = new FileAssetProcessingEngine(storagePort, antivirusPort);
        FileAsset asset = uploadedAsset(120);

        when(storagePort.exists(asset.getTempObjectKey())).thenReturn(true);
        when(storagePort.openStream(asset.getTempObjectKey())).thenReturn(new ByteArrayInputStream("data".getBytes()));
        when(antivirusPort.capabilities()).thenReturn(new AntivirusCapabilities(10_000));
        when(antivirusPort.scan(any(), eq(asset.getSizeBytes()), eq(asset.getOriginalName()), eq(asset.getDeclaredContentType())))
            .thenReturn(FileAssetAntivirusPort.ScanVerdict.infected("EICAR-Test-File"));

        assertThatThrownBy(() -> engine.process(asset, new V2Policy()))
            .isInstanceOf(AppException.class)
            .hasMessageContaining("Signature");
    }

    @Test
    @DisplayName("fails closed when antivirus is unavailable")
    void failsClosedWhenAntivirusUnavailable() {
        FileAssetProcessingEngine engine = new FileAssetProcessingEngine(storagePort, antivirusPort);
        FileAsset asset = uploadedAsset(120);

        when(storagePort.exists(asset.getTempObjectKey())).thenReturn(true);
        when(storagePort.openStream(asset.getTempObjectKey())).thenReturn(new ByteArrayInputStream("data".getBytes()));
        when(antivirusPort.capabilities()).thenReturn(new AntivirusCapabilities(10_000));
        when(antivirusPort.scan(any(), eq(asset.getSizeBytes()), eq(asset.getOriginalName()), eq(asset.getDeclaredContentType())))
            .thenReturn(FileAssetAntivirusPort.ScanVerdict.error(ScanFailureReason.UNAVAILABLE));

        assertThatThrownBy(() -> engine.process(asset, new V2Policy()))
            .isInstanceOf(AppException.class)
            .hasMessageContaining("Antivirus service is unavailable");
    }

    private static FileAsset uploadedAsset(long sizeBytes) {
        return FileAsset.builder()
            .id(UUID.randomUUID())
            .policyKey(FilePolicyKey.CONTROLLED_ATTACHMENT)
            .policyVersion(2)
            .status(FileAssetStatus.UPLOADED)
            .originalName("report.pdf")
            .declaredContentType("application/pdf")
            .sizeBytes(sizeBytes)
            .tempObjectKey("fileassets/quarantine/" + UUID.randomUUID())
            .uploadedAt(LocalDateTime.now())
            .createdAt(LocalDateTime.now())
            .build();
    }

    private static final class V2Policy implements FileSecurityPolicy {

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
            return Duration.ofHours(1);
        }

        @Override
        public Duration uploadedTtl() {
            return Duration.ofHours(1);
        }

        @Override
        public Duration activeUnboundTtl() {
            return Duration.ofDays(1);
        }

        @Override
        public int maxProcessingAttempts() {
            return 3;
        }

        @Override
        public long maxSizeBytes() {
            return 3L * 1024 * 1024 * 1024;
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

        @Override
        public FileAssetSafetyClass classify(FileAsset asset) {
            return FileAssetSafetyClass.CONTROLLED_ATTACHMENT_ONLY;
        }
    }
}
