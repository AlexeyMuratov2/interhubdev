package com.example.interhubdev.document.internal.storedFile;

import com.example.interhubdev.document.DocumentApi;
import com.example.interhubdev.document.DocumentStoredFileUsagePort;
import com.example.interhubdev.document.FileUploadInput;
import com.example.interhubdev.document.StoredFileDto;
import com.example.interhubdev.document.api.StoredFileDownloadAccessPort;
import com.example.interhubdev.error.Errors;
import com.example.interhubdev.storedfile.StoredFileApi;
import com.example.interhubdev.storedfile.StoredFileMeta;
import com.example.interhubdev.storedfile.StoredFileInput;
import com.example.interhubdev.user.Role;
import com.example.interhubdev.user.UserApi;
import com.example.interhubdev.user.UserDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Implementation of {@link DocumentApi}: delegates storage to {@link StoredFileApi}, enforces access control.
 */
@Service
@RequiredArgsConstructor
@Slf4j
class DocumentServiceImpl implements DocumentApi {

    private final StoredFileApi storedFileApi;
    private final UserApi userApi;
    private final DocumentStoredFileUsagePort documentStoredFileUsagePort;
    private final List<StoredFileDownloadAccessPort> storedFileDownloadAccessPorts;

    @Value("${app.storage.preview-url-expires-seconds:3600}")
    private int previewUrlExpiresSeconds;

    @Override
    @Transactional
    public StoredFileDto uploadFile(java.nio.file.Path tempFile, String originalFilename, String contentType, long size, UUID uploadedBy) {
        StoredFileMeta meta = storedFileApi.upload(tempFile, originalFilename, contentType, size, uploadedBy);
        return StoredFileMappers.fromMeta(meta);
    }

    @Override
    @Transactional
    public List<StoredFileDto> uploadFiles(List<FileUploadInput> inputs, UUID uploadedBy) {
        if (inputs == null || inputs.isEmpty()) {
            throw Errors.badRequest("At least one file is required");
        }
        List<StoredFileInput> storedInputs = inputs.stream()
            .map(i -> new StoredFileInput(i.tempPath(), i.originalFilename(), i.contentType(), i.size()))
            .collect(Collectors.toList());
        List<StoredFileMeta> metas = storedFileApi.uploadBatch(storedInputs, uploadedBy);
        return metas.stream().map(StoredFileMappers::fromMeta).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<StoredFileDto> getStoredFile(UUID id) {
        return storedFileApi.getMetadata(id).map(StoredFileMappers::fromMeta);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<UUID, StoredFileDto> getStoredFiles(Set<UUID> ids) {
        if (ids == null || ids.isEmpty()) {
            return Map.of();
        }
        return storedFileApi.getMetadataBatch(ids).entrySet().stream()
            .collect(Collectors.toMap(Map.Entry::getKey, e -> StoredFileMappers.fromMeta(e.getValue())));
    }

    @Override
    @Transactional(readOnly = true)
    public InputStream downloadByStoredFileId(UUID id, UUID currentUserId) {
        StoredFileMeta meta = storedFileApi.getMetadataOrThrow(id);
        checkAccessPermission(meta, currentUserId);
        return storedFileApi.getContent(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<String> getPreviewUrl(UUID storedFileId, int expiresSeconds, UUID currentUserId) {
        Optional<StoredFileMeta> opt = storedFileApi.getMetadata(storedFileId);
        if (opt.isEmpty()) {
            return Optional.empty();
        }
        checkAccessPermission(opt.get(), currentUserId);
        return storedFileApi.getPresignedUrl(storedFileId, expiresSeconds);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<String> getDownloadUrl(UUID storedFileId, int expiresSeconds, UUID currentUserId) {
        Optional<StoredFileMeta> opt = storedFileApi.getMetadata(storedFileId);
        if (opt.isEmpty()) {
            return Optional.empty();
        }
        checkAccessPermission(opt.get(), currentUserId);
        return storedFileApi.getPresignedUrl(storedFileId, expiresSeconds);
    }

    @Override
    @Transactional
    public void deleteStoredFile(UUID id, UUID currentUserId) {
        StoredFileMeta meta = storedFileApi.getMetadataOrThrow(id);
        checkDeletePermission(meta, currentUserId);
        storedFileApi.delete(id);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isStoredFileInUse(UUID storedFileId) {
        return documentStoredFileUsagePort.isStoredFileInUse(storedFileId);
    }

    private void checkAccessPermission(StoredFileMeta meta, UUID currentUserId) {
        if (meta.uploadedBy() != null && meta.uploadedBy().equals(currentUserId)) {
            return;
        }
        UserDto user = userApi.findById(currentUserId)
            .orElseThrow(() -> DocumentErrors.accessDenied());
        if (user.hasRole(Role.ADMIN) || user.hasRole(Role.MODERATOR) || user.hasRole(Role.SUPER_ADMIN)) {
            return;
        }
        for (StoredFileDownloadAccessPort port : storedFileDownloadAccessPorts) {
            if (port.canDownload(meta.id(), currentUserId)) {
                return;
            }
        }
        throw DocumentErrors.accessDenied();
    }

    private void checkDeletePermission(StoredFileMeta meta, UUID currentUserId) {
        if (meta.uploadedBy() != null && meta.uploadedBy().equals(currentUserId)) {
            return;
        }
        UserDto user = userApi.findById(currentUserId)
            .orElseThrow(() -> DocumentErrors.accessDenied());
        if (user.hasRole(Role.ADMIN) || user.hasRole(Role.MODERATOR) || user.hasRole(Role.SUPER_ADMIN)) {
            return;
        }
        throw DocumentErrors.accessDenied();
    }
}
