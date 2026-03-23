package com.example.interhubdev.fileasset.internal;

import com.example.interhubdev.error.AppException;
import com.example.interhubdev.fileasset.FileAssetApi;
import com.example.interhubdev.fileasset.FileAssetStatus;
import com.example.interhubdev.fileasset.FileAssetView;
import com.example.interhubdev.fileasset.FilePolicyKey;
import com.example.interhubdev.fileasset.FileUploadReceipt;
import com.example.interhubdev.fileasset.internal.FileAssetProcessingEngine.ProcessingResult;
import com.example.interhubdev.fileasset.internal.integration.FileAssetProcessingRequestedEventPayload;
import com.example.interhubdev.fileasset.internal.policy.FilePolicyRegistry;
import com.example.interhubdev.fileasset.internal.policy.FileSecurityPolicy;
import com.example.interhubdev.outbox.OutboxEventDraft;
import com.example.interhubdev.outbox.OutboxIntegrationEventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Internal implementation of the fileasset public API and lifecycle state machine.
 */
@Service
@RequiredArgsConstructor
@Slf4j
class FileAssetServiceImpl implements FileAssetApi {

    private static final Set<FileAssetStatus> EXPIRABLE_STATUSES = Set.of(
        FileAssetStatus.REGISTERED,
        FileAssetStatus.UPLOADED,
        FileAssetStatus.ACTIVE
    );

    private final FileAssetRepository fileAssetRepository;
    private final FilePolicyRegistry filePolicyRegistry;
    private final OutboxIntegrationEventPublisher outboxPublisher;
    private final FileAssetProcessingEngine processingEngine;
    private final FileAssetStoragePort storagePort;
    private final FileAssetCapacityGate capacityGate;

    @Override
    @Transactional
    public FileAssetView register(
        FilePolicyKey policyKey,
        String originalName,
        String declaredContentType,
        long sizeBytes,
        UUID uploadedBy
    ) {
        FileSecurityPolicy policy = filePolicyRegistry.resolveCurrent(policyKey);
        policy.validateRegistration(originalName, declaredContentType, sizeBytes);
        capacityGate.ensureWithinEffectiveLimit(policy, sizeBytes);

        LocalDateTime now = LocalDateTime.now();
        FileAsset entity = FileAsset.builder()
            .id(UUID.randomUUID())
            .policyKey(policy.key())
            .policyVersion(policy.version())
            .status(FileAssetStatus.REGISTERED)
            .originalName(originalName.trim())
            .declaredContentType(normalizeContentType(declaredContentType))
            .sizeBytes(sizeBytes)
            .uploadedBy(uploadedBy)
            .deliveryProfile(policy.deliveryProfile())
            .archiveProfile(policy.archiveProfile())
            .expiresAt(now.plus(policy.registeredTtl()))
            .createdAt(now)
            .build();

        return FileAssetMapper.toView(fileAssetRepository.save(entity));
    }

    @Override
    @Transactional
    public FileAssetView markUploaded(UUID fileAssetId, FileUploadReceipt uploadReceipt) {
        validateUploadReceipt(uploadReceipt);
        FileAsset entity = loadForUpdate(fileAssetId);

        if (entity.getStatus().isTerminal()) {
            throw FileAssetErrors.invalidTransition(fileAssetId, entity.getStatus(), "mark uploaded");
        }

        if (entity.getStatus() == FileAssetStatus.UPLOADED
            || entity.getStatus() == FileAssetStatus.PROCESSING
            || entity.getStatus() == FileAssetStatus.ACTIVE) {
            if (matchesExistingReceipt(entity, uploadReceipt)) {
                return FileAssetMapper.toView(entity);
            }
            throw FileAssetErrors.uploadReceiptConflict(fileAssetId);
        }

        if (entity.getStatus() != FileAssetStatus.REGISTERED) {
            throw FileAssetErrors.invalidTransition(fileAssetId, entity.getStatus(), "mark uploaded");
        }

        FileSecurityPolicy policy = filePolicyRegistry.resolveExact(entity.getPolicyKey(), entity.getPolicyVersion());
        LocalDateTime now = LocalDateTime.now();

        entity.setStatus(FileAssetStatus.UPLOADED);
        entity.setUploadReceiptToken(trimToNull(uploadReceipt.uploadToken()));
        entity.setChecksum(trimToNull(uploadReceipt.checksum()));
        entity.setEtag(trimToNull(uploadReceipt.etag()));
        entity.setUploadedAt(now);
        entity.setTempObjectKey(buildTempObjectKey(entity.getId()));
        entity.setExpiresAt(now.plus(policy.uploadedTtl()));

        return FileAssetMapper.toView(fileAssetRepository.save(entity));
    }

    @Override
    @Transactional
    public FileAssetView requestProcessing(UUID fileAssetId) {
        FileAsset entity = loadForUpdate(fileAssetId);

        if (entity.getStatus().isTerminal()) {
            throw FileAssetErrors.invalidTransition(fileAssetId, entity.getStatus(), "request processing");
        }
        if (entity.getStatus() == FileAssetStatus.ACTIVE || entity.getStatus() == FileAssetStatus.PROCESSING) {
            return FileAssetMapper.toView(entity);
        }
        if (entity.getStatus() != FileAssetStatus.UPLOADED) {
            throw FileAssetErrors.invalidTransition(fileAssetId, entity.getStatus(), "request processing");
        }

        entity.setStatus(FileAssetStatus.PROCESSING);
        FileAsset saved = fileAssetRepository.save(entity);

        outboxPublisher.publish(OutboxEventDraft.builder()
            .eventType(FileAssetEventTypes.PROCESSING_REQUESTED)
            .payload(new FileAssetProcessingRequestedEventPayload(fileAssetId))
            .occurredAt(Instant.now())
            .build());

        return FileAssetMapper.toView(saved);
    }

    @Override
    @Transactional
    public FileAssetView confirmBound(UUID fileAssetId) {
        FileAsset entity = loadForUpdate(fileAssetId);
        if (entity.getStatus().isTerminal()) {
            throw FileAssetErrors.invalidTransition(fileAssetId, entity.getStatus(), "confirm bound");
        }
        if (entity.getClaimedAt() != null) {
            return FileAssetMapper.toView(entity);
        }

        entity.setClaimedAt(LocalDateTime.now());
        if (entity.getStatus() == FileAssetStatus.ACTIVE) {
            entity.setExpiresAt(null);
        }

        return FileAssetMapper.toView(fileAssetRepository.save(entity));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<FileAssetView> get(UUID fileAssetId) {
        return fileAssetRepository.findById(fileAssetId).map(FileAssetMapper::toView);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<UUID, FileAssetView> getMany(Set<UUID> fileAssetIds) {
        if (fileAssetIds == null || fileAssetIds.isEmpty()) {
            return Map.of();
        }
        return fileAssetRepository.findAllById(fileAssetIds).stream()
            .collect(Collectors.toMap(FileAsset::getId, FileAssetMapper::toView));
    }

    @Override
    @Transactional
    public FileAssetView markDeleted(UUID fileAssetId) {
        FileAsset entity = loadForUpdate(fileAssetId);
        if (entity.getStatus() == FileAssetStatus.DELETED) {
            return FileAssetMapper.toView(entity);
        }
        if (entity.getStatus().isTerminal()) {
            throw FileAssetErrors.invalidTransition(fileAssetId, entity.getStatus(), "mark deleted");
        }

        entity.setStatus(FileAssetStatus.DELETED);
        entity.setDeletedAt(LocalDateTime.now());
        entity.setExpiresAt(null);
        cleanupPhysical(entity);

        return FileAssetMapper.toView(fileAssetRepository.save(entity));
    }

    @Transactional
    void handleProcessingRequested(UUID fileAssetId) {
        FileAsset entity = loadForUpdate(fileAssetId);

        if (entity.getStatus().isTerminal() || entity.getStatus() == FileAssetStatus.ACTIVE) {
            return;
        }
        if (entity.getStatus() != FileAssetStatus.PROCESSING) {
            log.debug("Ignoring processing event for fileAssetId={} in state={}", fileAssetId, entity.getStatus());
            return;
        }

        FileSecurityPolicy policy = filePolicyRegistry.resolveExact(entity.getPolicyKey(), entity.getPolicyVersion());
        int nextAttempt = entity.getProcessingAttempts() + 1;
        entity.setProcessingAttempts(nextAttempt);

        try {
            ProcessingResult result = processingEngine.process(entity, policy);
            applyProcessingSuccess(entity, result);
            fileAssetRepository.save(entity);
        } catch (RuntimeException ex) {
            applyProcessingFailure(entity, policy, ex);
        }
    }

    @Transactional
    void expireStaleAssets(LocalDateTime now) {
        var staleAssets = fileAssetRepository.findExpirableAssetsForUpdate(EXPIRABLE_STATUSES, now);
        for (FileAsset entity : staleAssets) {
            if (entity.getStatus().isTerminal()) {
                continue;
            }
            if (entity.getStatus() == FileAssetStatus.ACTIVE && entity.getClaimedAt() != null) {
                continue;
            }
            entity.setStatus(FileAssetStatus.EXPIRED);
            entity.setExpiresAt(now);
            cleanupPhysical(entity);
            fileAssetRepository.save(entity);
        }
    }

    private void applyProcessingSuccess(FileAsset entity, ProcessingResult result) {
        entity.setStatus(FileAssetStatus.ACTIVE);
        entity.setDetectedContentType(result.detectedContentType());
        entity.setFinalObjectKey(result.finalObjectKey());
        entity.setSafetyClass(result.safetyClass());
        entity.setDeliveryProfile(result.deliveryProfile());
        entity.setArchiveProfile(result.archiveProfile());
        entity.setActivatedAt(LocalDateTime.now());
        entity.setFailedAt(null);
        entity.setLastFailureCode(null);
        entity.setLastFailureMessage(null);
        entity.setExpiresAt(result.nextExpiresAt());
        entity.setTempObjectKey(null);
    }

    private void applyProcessingFailure(FileAsset entity, FileSecurityPolicy policy, RuntimeException ex) {
        entity.setLastFailureCode(extractFailureCode(ex));
        entity.setLastFailureMessage(truncateFailureMessage(ex));

        if (entity.getProcessingAttempts() >= policy.maxProcessingAttempts()) {
            entity.setStatus(FileAssetStatus.FAILED);
            entity.setFailedAt(LocalDateTime.now());
            entity.setExpiresAt(null);
            cleanupPhysical(entity);
            fileAssetRepository.save(entity);
            return;
        }

        fileAssetRepository.save(entity);
        throw ex;
    }

    private FileAsset loadForUpdate(UUID fileAssetId) {
        return fileAssetRepository.findByIdForUpdate(fileAssetId)
            .orElseThrow(() -> FileAssetErrors.fileAssetNotFound(fileAssetId));
    }

    private static void validateUploadReceipt(FileUploadReceipt uploadReceipt) {
        if (uploadReceipt == null) {
            throw FileAssetErrors.invalidUploadReceipt();
        }
        boolean hasToken = uploadReceipt.uploadToken() != null && !uploadReceipt.uploadToken().isBlank();
        boolean hasChecksum = uploadReceipt.checksum() != null && !uploadReceipt.checksum().isBlank();
        boolean hasEtag = uploadReceipt.etag() != null && !uploadReceipt.etag().isBlank();
        if (!hasToken && !hasChecksum && !hasEtag) {
            throw FileAssetErrors.invalidUploadReceipt();
        }
    }

    private static boolean matchesExistingReceipt(FileAsset entity, FileUploadReceipt uploadReceipt) {
        return sameValue(entity.getUploadReceiptToken(), uploadReceipt.uploadToken())
            && sameValue(entity.getChecksum(), uploadReceipt.checksum())
            && sameValue(entity.getEtag(), uploadReceipt.etag());
    }

    private static boolean sameValue(String current, String candidate) {
        String normalizedCurrent = trimToNull(current);
        String normalizedCandidate = trimToNull(candidate);
        return normalizedCurrent == null ? normalizedCandidate == null : normalizedCurrent.equals(normalizedCandidate);
    }

    private static String normalizeContentType(String declaredContentType) {
        if (declaredContentType == null || declaredContentType.isBlank()) {
            return "application/octet-stream";
        }
        return declaredContentType.split(";")[0].trim().toLowerCase();
    }

    private static String buildTempObjectKey(UUID fileAssetId) {
        return "fileassets/quarantine/" + fileAssetId;
    }

    private void cleanupPhysical(FileAsset entity) {
        storagePort.deleteQuietly(entity.getTempObjectKey());
        storagePort.deleteQuietly(entity.getFinalObjectKey());
        entity.setTempObjectKey(null);
        entity.setFinalObjectKey(null);
    }

    private static String extractFailureCode(RuntimeException ex) {
        if (ex instanceof AppException appException) {
            return appException.getCode();
        }
        return "FILE_ASSET_PROCESSING_ERROR";
    }

    private static String truncateFailureMessage(RuntimeException ex) {
        String message = ex.getMessage();
        if (message == null || message.isBlank()) {
            message = ex.getClass().getSimpleName();
        }
        if (message.length() > 1000) {
            return message.substring(0, 1000);
        }
        return message;
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
