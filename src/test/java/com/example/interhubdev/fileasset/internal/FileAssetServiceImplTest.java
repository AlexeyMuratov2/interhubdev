package com.example.interhubdev.fileasset.internal;

import com.example.interhubdev.error.AppException;
import com.example.interhubdev.fileasset.FileAssetArchiveProfile;
import com.example.interhubdev.fileasset.FileAssetSafetyClass;
import com.example.interhubdev.fileasset.FileAssetStatus;
import com.example.interhubdev.fileasset.FileAssetView;
import com.example.interhubdev.fileasset.FileDeliveryProfile;
import com.example.interhubdev.fileasset.FilePolicyKey;
import com.example.interhubdev.fileasset.FileUploadReceipt;
import com.example.interhubdev.fileasset.internal.policy.FilePolicyRegistry;
import com.example.interhubdev.fileasset.internal.policy.FileSecurityPolicy;
import com.example.interhubdev.fileasset.internal.policy.AntivirusMode;
import com.example.interhubdev.outbox.OutboxEventDraft;
import com.example.interhubdev.outbox.OutboxIntegrationEventPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("FileAssetServiceImpl")
class FileAssetServiceImplTest {

    private static final UUID FILE_ID = UUID.randomUUID();
    private static final UUID USER_ID = UUID.randomUUID();

    @Mock
    private FileAssetRepository fileAssetRepository;
    @Mock
    private OutboxIntegrationEventPublisher outboxPublisher;
    @Mock
    private FileAssetProcessingEngine processingEngine;
    @Mock
    private FileAssetStoragePort storagePort;

    private FileAssetServiceImpl service;

    @BeforeEach
    void setUp() {
        FilePolicyRegistry registry = new FilePolicyRegistry(List.of(
            new TestPolicy(FilePolicyKey.CONTROLLED_ATTACHMENT, 1, Duration.ofHours(24), Duration.ofHours(12), Duration.ofDays(7)),
            new TestPolicy(FilePolicyKey.CONTROLLED_ATTACHMENT, 2, Duration.ofHours(48), Duration.ofHours(24), Duration.ofDays(10))
        ));
        service = new FileAssetServiceImpl(
            fileAssetRepository,
            registry,
            outboxPublisher,
            processingEngine,
            storagePort,
            new FileAssetCapacityGate(10L * 1024 * 1024 * 1024, 10L * 1024 * 1024 * 1024)
        );
    }

    @Nested
    @DisplayName("register")
    class Register {

        @Test
        @DisplayName("pins the current policy version and computes expiry from it")
        void pinsCurrentPolicyVersion() {
            when(fileAssetRepository.save(any(FileAsset.class))).thenAnswer(invocation -> invocation.getArgument(0));

            FileAssetView view = service.register(
                FilePolicyKey.CONTROLLED_ATTACHMENT,
                "report.pdf",
                "application/pdf",
                1024,
                USER_ID
            );

            ArgumentCaptor<FileAsset> captor = ArgumentCaptor.forClass(FileAsset.class);
            verify(fileAssetRepository).save(captor.capture());
            FileAsset saved = captor.getValue();

            assertThat(saved.getPolicyVersion()).isEqualTo(2);
            assertThat(saved.getStatus()).isEqualTo(FileAssetStatus.REGISTERED);
            assertThat(saved.getExpiresAt()).isNotNull();
            assertThat(view.policyVersion()).isEqualTo(2);
            assertThat(view.status()).isEqualTo(FileAssetStatus.REGISTERED);
        }

        @Test
        @DisplayName("rejects registration above current runtime capacity even if policy ceiling is higher")
        void rejectsWhenCapacityGateIsLowerThanPolicyCeiling() {
            FilePolicyRegistry registry = new FilePolicyRegistry(List.of(
                new FileSecurityPolicy() {
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
                        return 10_000;
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
                }
            ));
            FileAssetServiceImpl limitedService = new FileAssetServiceImpl(
                fileAssetRepository,
                registry,
                outboxPublisher,
                processingEngine,
                storagePort,
                new FileAssetCapacityGate(4_096, 2_048)
            );

            assertThatThrownBy(() -> limitedService.register(
                FilePolicyKey.CONTROLLED_ATTACHMENT,
                "archive.zip",
                "application/zip",
                3_000,
                USER_ID
            ))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("current runtime capacity");
        }
    }

    @Nested
    @DisplayName("markUploaded")
    class MarkUploaded {

        @Test
        @DisplayName("is idempotent for the same upload receipt on an already uploaded asset")
        void idempotentForSameReceipt() {
            FileAsset entity = uploadedAsset();
            when(fileAssetRepository.findByIdForUpdate(FILE_ID)).thenReturn(Optional.of(entity));

            FileAssetView view = service.markUploaded(FILE_ID, new FileUploadReceipt("token-1", "sha256", "etag-1"));

            assertThat(view.status()).isEqualTo(FileAssetStatus.UPLOADED);
            verify(fileAssetRepository, never()).save(any(FileAsset.class));
        }

        @Test
        @DisplayName("rejects different receipt after upload was already confirmed")
        void rejectsDifferentReceipt() {
            FileAsset entity = uploadedAsset();
            when(fileAssetRepository.findByIdForUpdate(FILE_ID)).thenReturn(Optional.of(entity));

            assertThatThrownBy(() -> service.markUploaded(FILE_ID, new FileUploadReceipt("other-token", "other", "other")))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("Upload receipt does not match");
        }
    }

    @Nested
    @DisplayName("requestProcessing")
    class RequestProcessing {

        @Test
        @DisplayName("moves uploaded asset to PROCESSING and publishes outbox event")
        void movesToProcessing() {
            FileAsset entity = uploadedAsset();
            when(fileAssetRepository.findByIdForUpdate(FILE_ID)).thenReturn(Optional.of(entity));
            when(fileAssetRepository.save(any(FileAsset.class))).thenAnswer(invocation -> invocation.getArgument(0));

            FileAssetView view = service.requestProcessing(FILE_ID);

            assertThat(view.status()).isEqualTo(FileAssetStatus.PROCESSING);
            verify(fileAssetRepository).save(entity);
            verify(outboxPublisher).publish(any(OutboxEventDraft.class));
        }

        @Test
        @DisplayName("rejects requestProcessing before bytes are uploaded")
        void rejectsFromRegistered() {
            FileAsset entity = registeredAsset();
            when(fileAssetRepository.findByIdForUpdate(FILE_ID)).thenReturn(Optional.of(entity));

            assertThatThrownBy(() -> service.requestProcessing(FILE_ID))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("request processing");
        }
    }

    @Test
    @DisplayName("confirmBound clears expiry for active asset and is idempotent")
    void confirmBoundClearsExpiry() {
        FileAsset entity = activeAsset();
        when(fileAssetRepository.findByIdForUpdate(FILE_ID)).thenReturn(Optional.of(entity));
        when(fileAssetRepository.save(any(FileAsset.class))).thenAnswer(invocation -> invocation.getArgument(0));

        FileAssetView first = service.confirmBound(FILE_ID);
        FileAssetView second = service.confirmBound(FILE_ID);

        assertThat(first.claimedAt()).isNotNull();
        assertThat(first.expiresAt()).isNull();
        assertThat(second.claimedAt()).isEqualTo(first.claimedAt());
    }

    @Nested
    @DisplayName("handleProcessingRequested")
    class HandleProcessingRequested {

        @Test
        @DisplayName("activates processing asset with pinned policy version")
        void activatesUsingPinnedPolicy() {
            FileAsset entity = processingAsset();
            entity.setPolicyVersion(1);
            when(fileAssetRepository.findByIdForUpdate(FILE_ID)).thenReturn(Optional.of(entity));
            when(fileAssetRepository.save(any(FileAsset.class))).thenAnswer(invocation -> invocation.getArgument(0));
            when(processingEngine.process(eq(entity), any(FileSecurityPolicy.class)))
                .thenReturn(new FileAssetProcessingEngine.ProcessingResult(
                    "application/pdf",
                    "fileassets/final/2026/3/" + FILE_ID + "_report.pdf",
                    FileAssetSafetyClass.CONTROLLED_ATTACHMENT_ONLY,
                    FileDeliveryProfile.CONTROLLED_ATTACHMENT_ONLY,
                    FileAssetArchiveProfile.STANDARD,
                    LocalDateTime.now().plusDays(7)
                ));

            service.handleProcessingRequested(FILE_ID);

            ArgumentCaptor<FileSecurityPolicy> policyCaptor = ArgumentCaptor.forClass(FileSecurityPolicy.class);
            verify(processingEngine).process(eq(entity), policyCaptor.capture());
            assertThat(policyCaptor.getValue().version()).isEqualTo(1);
            assertThat(entity.getStatus()).isEqualTo(FileAssetStatus.ACTIVE);
            assertThat(entity.getProcessingAttempts()).isEqualTo(1);
            assertThat(entity.getActivatedAt()).isNotNull();
        }

        @Test
        @DisplayName("rethrows transient processing failure while keeping asset in PROCESSING")
        void rethrowsTransientFailure() {
            FileAsset entity = processingAsset();
            entity.setProcessingAttempts(1);
            when(fileAssetRepository.findByIdForUpdate(FILE_ID)).thenReturn(Optional.of(entity));
            when(fileAssetRepository.save(any(FileAsset.class))).thenAnswer(invocation -> invocation.getArgument(0));
            when(processingEngine.process(eq(entity), any(FileSecurityPolicy.class)))
                .thenThrow(new RuntimeException("scanner unavailable"));

            assertThatThrownBy(() -> service.handleProcessingRequested(FILE_ID))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("scanner unavailable");

            assertThat(entity.getStatus()).isEqualTo(FileAssetStatus.PROCESSING);
            assertThat(entity.getProcessingAttempts()).isEqualTo(2);
            assertThat(entity.getLastFailureMessage()).contains("scanner unavailable");
            verify(storagePort, never()).deleteQuietly(any());
        }

        @Test
        @DisplayName("marks asset as FAILED when retry budget is exhausted")
        void marksFailedAtRetryBudget() {
            FileAsset entity = processingAsset();
            entity.setProcessingAttempts(2);
            when(fileAssetRepository.findByIdForUpdate(FILE_ID)).thenReturn(Optional.of(entity));
            when(fileAssetRepository.save(any(FileAsset.class))).thenAnswer(invocation -> invocation.getArgument(0));
            when(processingEngine.process(eq(entity), any(FileSecurityPolicy.class)))
                .thenThrow(new RuntimeException("hard failure"));

            service.handleProcessingRequested(FILE_ID);

            assertThat(entity.getStatus()).isEqualTo(FileAssetStatus.FAILED);
            assertThat(entity.getProcessingAttempts()).isEqualTo(3);
            assertThat(entity.getFailedAt()).isNotNull();
            verify(storagePort).deleteQuietly("fileassets/quarantine/" + FILE_ID);
        }
    }

    @Test
    @DisplayName("expireStaleAssets moves orphaned assets to EXPIRED")
    void expireStaleAssets() {
        FileAsset entity = uploadedAsset();
        entity.setExpiresAt(LocalDateTime.now().minusMinutes(5));
        when(fileAssetRepository.findExpirableAssetsForUpdate(any(Set.class), any(LocalDateTime.class)))
            .thenReturn(List.of(entity));
        when(fileAssetRepository.save(any(FileAsset.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.expireStaleAssets(LocalDateTime.now());

        assertThat(entity.getStatus()).isEqualTo(FileAssetStatus.EXPIRED);
        verify(storagePort).deleteQuietly("fileassets/quarantine/" + FILE_ID);
    }

    private static FileAsset registeredAsset() {
        return FileAsset.builder()
            .id(FILE_ID)
            .policyKey(FilePolicyKey.CONTROLLED_ATTACHMENT)
            .policyVersion(1)
            .status(FileAssetStatus.REGISTERED)
            .originalName("report.pdf")
            .declaredContentType("application/pdf")
            .sizeBytes(1024)
            .uploadedBy(USER_ID)
            .expiresAt(LocalDateTime.now().plusHours(24))
            .createdAt(LocalDateTime.now())
            .build();
    }

    private static FileAsset uploadedAsset() {
        FileAsset entity = registeredAsset();
        entity.setStatus(FileAssetStatus.UPLOADED);
        entity.setUploadReceiptToken("token-1");
        entity.setChecksum("sha256");
        entity.setEtag("etag-1");
        entity.setUploadedAt(LocalDateTime.now());
        entity.setTempObjectKey("fileassets/quarantine/" + FILE_ID);
        return entity;
    }

    private static FileAsset processingAsset() {
        FileAsset entity = uploadedAsset();
        entity.setStatus(FileAssetStatus.PROCESSING);
        return entity;
    }

    private static FileAsset activeAsset() {
        FileAsset entity = processingAsset();
        entity.setStatus(FileAssetStatus.ACTIVE);
        entity.setActivatedAt(LocalDateTime.now());
        entity.setExpiresAt(LocalDateTime.now().plusDays(1));
        return entity;
    }

    private record TestPolicy(
        FilePolicyKey key,
        int version,
        Duration registeredTtl,
        Duration uploadedTtl,
        Duration activeUnboundTtl
    ) implements FileSecurityPolicy {

        @Override
        public int maxProcessingAttempts() {
            return 3;
        }

        @Override
        public long maxSizeBytes() {
            return 1024L * 1024L;
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
        public FileAssetSafetyClass classify(FileAsset asset) {
            return FileAssetSafetyClass.CONTROLLED_ATTACHMENT_ONLY;
        }
    }
}
