package com.example.interhubdev.document.internal.attachment;

import com.example.interhubdev.document.DocumentAttachmentApi;
import com.example.interhubdev.document.DocumentAttachmentDto;
import com.example.interhubdev.error.Errors;
import com.example.interhubdev.fileasset.FileAssetApi;
import com.example.interhubdev.fileasset.FileAssetDownloadHandle;
import com.example.interhubdev.fileasset.FileAssetUploadCommand;
import com.example.interhubdev.fileasset.FileAssetView;
import com.example.interhubdev.user.UserApi;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DocumentAttachmentService implements DocumentAttachmentApi {

    private final DocumentAttachmentRepository documentAttachmentRepository;
    private final FileAssetApi fileAssetApi;
    private final UserApi userApi;

    public List<DocumentAttachmentDto> createAttachments(
        DocumentAttachmentOwnerType ownerType,
        UUID ownerId,
        List<FileAssetUploadCommand> uploads
    ) {
        if (uploads == null || uploads.isEmpty()) {
            return List.of();
        }
        int nextOrder = documentAttachmentRepository.findByOwnerTypeAndOwnerIdOrderBySortOrderAsc(ownerType, ownerId).size();
        List<DocumentAttachment> created = new java.util.ArrayList<>(uploads.size());
        List<UUID> fileAssetIds = new java.util.ArrayList<>(uploads.size());
        for (FileAssetUploadCommand upload : uploads) {
            FileAssetView asset = fileAssetApi.ingest(upload);
            fileAssetIds.add(asset.id());
            created.add(documentAttachmentRepository.save(DocumentAttachment.builder()
                .id(UUID.randomUUID())
                .ownerType(ownerType)
                .ownerId(ownerId)
                .fileAssetId(asset.id())
                .sortOrder(nextOrder++)
                .build()));
        }
        confirmAfterCommit(fileAssetIds);
        return mapDtos(created);
    }

    public List<DocumentAttachmentDto> replaceAttachments(
        DocumentAttachmentOwnerType ownerType,
        UUID ownerId,
        List<UUID> retainAttachmentIds,
        boolean clearAttachments,
        List<FileAssetUploadCommand> uploads
    ) {
        List<DocumentAttachment> existing = documentAttachmentRepository.findByOwnerTypeAndOwnerIdOrderBySortOrderAsc(ownerType, ownerId);
        Set<UUID> retainedIds = retainAttachmentIds != null ? Set.copyOf(retainAttachmentIds) : Set.of();
        if (!clearAttachments && !retainedIds.isEmpty()) {
            Set<UUID> existingIds = existing.stream().map(DocumentAttachment::getId).collect(Collectors.toSet());
            if (!existingIds.containsAll(retainedIds)) {
                throw Errors.notFound("One or more retained attachments do not belong to owner " + ownerId);
            }
        }
        List<DocumentAttachment> kept = clearAttachments
            ? List.of()
            : existing.stream().filter(attachment -> retainedIds.contains(attachment.getId())).toList();
        List<DocumentAttachment> removed = clearAttachments
            ? existing
            : existing.stream().filter(attachment -> !retainedIds.contains(attachment.getId())).toList();

        for (DocumentAttachment attachment : removed) {
            documentAttachmentRepository.delete(attachment);
            deleteAfterCommit(attachment.getFileAssetId());
        }

        int order = 0;
        for (DocumentAttachment attachment : kept) {
            attachment.setSortOrder(order++);
            documentAttachmentRepository.save(attachment);
        }

        List<DocumentAttachmentDto> result = new java.util.ArrayList<>(mapDtos(kept));
        if (uploads != null && !uploads.isEmpty()) {
            List<DocumentAttachmentDto> appended = createAttachments(ownerType, ownerId, uploads);
            result.addAll(appended);
        }
        return result;
    }

    public List<DocumentAttachmentDto> findByOwner(DocumentAttachmentOwnerType ownerType, UUID ownerId) {
        return mapDtos(documentAttachmentRepository.findByOwnerTypeAndOwnerIdOrderBySortOrderAsc(ownerType, ownerId));
    }

    public Map<UUID, List<DocumentAttachmentDto>> findByOwners(DocumentAttachmentOwnerType ownerType, Collection<UUID> ownerIds) {
        if (ownerIds == null || ownerIds.isEmpty()) {
            return Map.of();
        }
        List<DocumentAttachment> attachments = documentAttachmentRepository.findByOwnerTypeAndOwnerIdInOrderByOwnerIdAndSortOrder(ownerType, ownerIds);
        Map<UUID, List<DocumentAttachment>> grouped = attachments.stream()
            .collect(Collectors.groupingBy(DocumentAttachment::getOwnerId, LinkedHashMap::new, Collectors.toList()));
        Set<UUID> fileAssetIds = attachments.stream().map(DocumentAttachment::getFileAssetId).collect(Collectors.toSet());
        Map<UUID, FileAssetView> assets = fileAssetApi.getMany(fileAssetIds);

        Map<UUID, List<DocumentAttachmentDto>> result = new LinkedHashMap<>();
        for (UUID ownerId : ownerIds) {
            List<DocumentAttachment> ownerAttachments = grouped.getOrDefault(ownerId, List.of());
            result.put(ownerId, ownerAttachments.stream()
                .map(attachment -> DocumentAttachmentMapper.toDto(attachment, assets.get(attachment.getFileAssetId())))
                .toList());
        }
        return result;
    }

    public void removeAttachment(DocumentAttachmentOwnerType ownerType, UUID ownerId, UUID attachmentId) {
        DocumentAttachment attachment = documentAttachmentRepository.findByIdAndOwnerType(attachmentId, ownerType)
            .filter(candidate -> candidate.getOwnerId().equals(ownerId))
            .orElseThrow(() -> Errors.notFound("Document attachment not found: " + attachmentId));
        documentAttachmentRepository.delete(attachment);
        deleteAfterCommit(attachment.getFileAssetId());
        reindex(ownerType, ownerId);
    }

    public void removeAll(DocumentAttachmentOwnerType ownerType, UUID ownerId) {
        List<DocumentAttachment> attachments = documentAttachmentRepository.findByOwnerTypeAndOwnerIdOrderBySortOrderAsc(ownerType, ownerId);
        if (attachments.isEmpty()) {
            return;
        }
        documentAttachmentRepository.deleteAll(attachments);
        attachments.forEach(attachment -> deleteAfterCommit(attachment.getFileAssetId()));
    }

    @Override
    public Optional<DocumentAttachmentDto> get(UUID attachmentId, UUID requesterId) {
        ensureAuthenticated(requesterId);
        return documentAttachmentRepository.findById(attachmentId)
            .map(attachment -> DocumentAttachmentMapper.toDto(
                attachment,
                fileAssetApi.get(attachment.getFileAssetId())
                    .orElseThrow(() -> Errors.notFound("File asset not found for attachment " + attachmentId))
            ));
    }

    @Override
    public FileAssetDownloadHandle download(UUID attachmentId, UUID requesterId) {
        ensureAuthenticated(requesterId);
        DocumentAttachment attachment = documentAttachmentRepository.findById(attachmentId)
            .orElseThrow(() -> Errors.notFound("Document attachment not found: " + attachmentId));
        return fileAssetApi.openDownload(attachment.getFileAssetId());
    }

    private List<DocumentAttachmentDto> mapDtos(List<DocumentAttachment> attachments) {
        if (attachments == null || attachments.isEmpty()) {
            return List.of();
        }
        Set<UUID> fileAssetIds = attachments.stream().map(DocumentAttachment::getFileAssetId).collect(Collectors.toSet());
        Map<UUID, FileAssetView> assets = fileAssetApi.getMany(fileAssetIds);
        return attachments.stream()
            .sorted(java.util.Comparator.comparingInt(DocumentAttachment::getSortOrder))
            .map(attachment -> DocumentAttachmentMapper.toDto(attachment, assets.get(attachment.getFileAssetId())))
            .toList();
    }

    private void reindex(DocumentAttachmentOwnerType ownerType, UUID ownerId) {
        List<DocumentAttachment> attachments = documentAttachmentRepository.findByOwnerTypeAndOwnerIdOrderBySortOrderAsc(ownerType, ownerId);
        for (int i = 0; i < attachments.size(); i++) {
            attachments.get(i).setSortOrder(i);
            documentAttachmentRepository.save(attachments.get(i));
        }
    }

    private void ensureAuthenticated(UUID requesterId) {
        userApi.findById(requesterId).orElseThrow(() -> Errors.unauthorized("Authentication required"));
    }

    private void confirmAfterCommit(List<UUID> fileAssetIds) {
        afterCommit(() -> {
            for (UUID fileAssetId : fileAssetIds) {
                try {
                    fileAssetApi.confirmBound(fileAssetId);
                } catch (Exception ignored) {
                    // Best-effort bind confirmation.
                }
            }
            return null;
        });
    }

    private void deleteAfterCommit(UUID fileAssetId) {
        afterCommit(() -> {
            try {
                fileAssetApi.markDeleted(fileAssetId);
            } catch (Exception ignored) {
                // Ignore "in use" / already deleted during cleanup.
            }
            return null;
        });
    }

    private static <T> T afterCommit(Supplier<T> afterCommitAction) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            return afterCommitAction.get();
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                afterCommitAction.get();
            }
        });
        return null;
    }
}
