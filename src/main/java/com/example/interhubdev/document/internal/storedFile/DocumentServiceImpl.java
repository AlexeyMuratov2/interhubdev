package com.example.interhubdev.document.internal.storedFile;

import com.example.interhubdev.document.DocumentApi;
import com.example.interhubdev.document.FileUploadInput;
import com.example.interhubdev.document.StoredFileDto;
import com.example.interhubdev.document.api.StoredFileDownloadAccessPort;
import com.example.interhubdev.document.api.StoredFileUsagePort;
import com.example.interhubdev.document.StoragePort;
import com.example.interhubdev.document.UploadContext;
import com.example.interhubdev.document.UploadResult;
import com.example.interhubdev.document.UploadSecurityPort;
import com.example.interhubdev.document.internal.courseMaterial.CourseMaterialRepository;
import com.example.interhubdev.document.internal.homework.HomeworkFileRepository;
import com.example.interhubdev.document.internal.lessonMaterial.LessonMaterialFileRepository;
import com.example.interhubdev.error.AppException;
import com.example.interhubdev.error.Errors;
import com.example.interhubdev.user.Role;
import com.example.interhubdev.user.UserApi;
import com.example.interhubdev.user.UserDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.io.InterruptedIOException;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Implementation of {@link DocumentApi}: file upload (atomic with S3), get, download, preview URL, delete.
 */
@Service
@RequiredArgsConstructor
@Slf4j
class DocumentServiceImpl implements DocumentApi {

    private final UploadSecurityPort uploadSecurityPort;
    private final StoragePort storagePort;
    private final StoredFileRepository storedFileRepository;
    private final FileValidation fileValidation;
    private final UserApi userApi;
    private final CourseMaterialRepository courseMaterialRepository;
    private final HomeworkFileRepository homeworkFileRepository;
    private final LessonMaterialFileRepository lessonMaterialFileRepository;
    private final List<StoredFileUsagePort> storedFileUsagePorts;
    private final List<StoredFileDownloadAccessPort> storedFileDownloadAccessPorts;

    @Value("${app.storage.preview-url-expires-seconds:3600}")
    private int previewUrlExpiresSeconds;

    @Value("${app.document.max-files-per-batch:50}")
    private int maxFilesPerBatch;

    @Override
    @Transactional
    public StoredFileDto uploadFile(Path tempFile, String originalFilename, String contentType, long size, UUID uploadedBy) {
        StoredFileAndPath result = uploadOneFileToStorageAndDb(tempFile, originalFilename, contentType, size, uploadedBy);
        return StoredFileMappers.toDto(result.entity());
    }

    @Override
    @Transactional
    public List<StoredFileDto> uploadFiles(List<FileUploadInput> inputs, UUID uploadedBy) {
        if (inputs == null || inputs.isEmpty()) {
            throw Errors.badRequest("At least one file is required");
        }
        if (inputs.size() > maxFilesPerBatch) {
            throw DocumentErrors.batchTooLarge(maxFilesPerBatch);
        }
        List<StoredFileAndPath> uploaded = new ArrayList<>();
        try {
            for (FileUploadInput input : inputs) {
                StoredFileAndPath result = uploadOneFileToStorageAndDb(
                    input.tempPath(), input.originalFilename(), input.contentType(), input.size(), uploadedBy);
                uploaded.add(result);
            }
            return uploaded.stream().map(s -> StoredFileMappers.toDto(s.entity())).toList();
        } catch (Exception e) {
            for (StoredFileAndPath u : uploaded) {
                try {
                    storedFileRepository.deleteById(u.entity().getId());
                } catch (Exception deleteEx) {
                    log.warn("Failed to delete StoredFile during batch rollback: {}", u.entity().getId(), deleteEx);
                }
                try {
                    storagePort.delete(u.storagePath());
                } catch (Exception deleteEx) {
                    log.warn("Failed to delete file from storage during batch rollback: {}", u.storagePath(), deleteEx);
                }
            }
            if (e instanceof AppException appEx) {
                throw appEx;
            }
            throw e;
        }
    }

    /**
     * Upload one file: security check, validation, S3 upload, DB save. Returns entity and storage path for rollback.
     */
    private StoredFileAndPath uploadOneFileToStorageAndDb(Path tempFile, String originalFilename,
                                                          String contentType, long size, UUID uploadedBy) {
        uploadSecurityPort.ensureUploadAllowed(UploadContext.of(uploadedBy, contentType, size, originalFilename), tempFile);
        fileValidation.validateUpload(size, contentType, originalFilename);
        String sanitizedName = sanitizeFilename(originalFilename);
        UUID id = UUID.randomUUID();
        YearMonth now = YearMonth.now();
        String path = "files/" + now.getYear() + "/" + now.getMonthValue() + "/" + id + "_" + sanitizedName;

        UploadResult uploadResult;
        try (InputStream stream = Files.newInputStream(tempFile)) {
            uploadResult = storagePort.upload(path, stream, contentType, size);
        } catch (Exception e) {
            if (e instanceof AppException appEx) {
                throw appEx;
            }
            log.error("Failed to upload file to storage: {}", path, e);
            if (isStorageUnavailable(e)) {
                throw DocumentErrors.storageUnavailable();
            }
            throw DocumentErrors.uploadFailed();
        }

        try {
            StoredFile entity = StoredFile.builder()
                .id(id)
                .storagePath(uploadResult.path())
                .size(uploadResult.size())
                .contentType(uploadResult.contentType())
                .originalName(originalFilename)
                .uploadedAt(LocalDateTime.now())
                .uploadedBy(uploadedBy)
                .build();
            storedFileRepository.save(entity);
            return new StoredFileAndPath(entity, path);
        } catch (Exception e) {
            log.warn("Failed to save StoredFile after S3 upload, removing file from storage: {}", path, e);
            try {
                storagePort.delete(path);
            } catch (Exception deleteEx) {
                log.error("Failed to delete file from storage during compensation: {}", path, deleteEx);
            }
            if (e instanceof AppException appEx) {
                throw appEx;
            }
            throw DocumentErrors.saveFailed();
        }
    }

    private record StoredFileAndPath(StoredFile entity, String storagePath) {
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<StoredFileDto> getStoredFile(UUID id) {
        return storedFileRepository.findById(id).map(StoredFileMappers::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public InputStream downloadByStoredFileId(UUID id, UUID currentUserId) {
        StoredFile entity = storedFileRepository.findById(id)
            .orElseThrow(() -> DocumentErrors.storedFileNotFound(id));
        checkAccessPermission(entity, currentUserId);
        try {
            return storagePort.download(entity.getStoragePath());
        } catch (Exception e) {
            if (e instanceof AppException appEx) {
                throw appEx;
            }
            log.error("Failed to download file from storage: {}", entity.getStoragePath(), e);
            if (isStorageUnavailable(e)) {
                throw DocumentErrors.storageUnavailable();
            }
            throw DocumentErrors.storedFileNotFound(id);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<String> getPreviewUrl(UUID storedFileId, int expiresSeconds, UUID currentUserId) {
        Optional<StoredFile> opt = storedFileRepository.findById(storedFileId);
        if (opt.isEmpty()) {
            return Optional.empty();
        }
        StoredFile entity = opt.get();
        checkAccessPermission(entity, currentUserId);
        Optional<String> url = storagePort.generatePreviewUrl(entity.getStoragePath(), expiresSeconds);
        if (url.isEmpty()) {
            throw DocumentErrors.fileNotFoundInStorage();
        }
        return url;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<String> getDownloadUrl(UUID storedFileId, int expiresSeconds, UUID currentUserId) {
        Optional<StoredFile> opt = storedFileRepository.findById(storedFileId);
        if (opt.isEmpty()) {
            return Optional.empty();
        }
        StoredFile entity = opt.get();
        checkAccessPermission(entity, currentUserId);
        Optional<String> url = storagePort.generatePreviewUrl(entity.getStoragePath(), expiresSeconds);
        if (url.isEmpty()) {
            throw DocumentErrors.fileNotFoundInStorage();
        }
        return url;
    }

    @Override
    @Transactional
    public void deleteStoredFile(UUID id, UUID currentUserId) {
        StoredFile entity = storedFileRepository.findById(id)
            .orElseThrow(() -> DocumentErrors.storedFileNotFound(id));
        checkDeletePermission(entity, currentUserId);
        checkFileNotInUse(id);
        String path = entity.getStoragePath();
        storedFileRepository.delete(entity);
        try {
            storagePort.delete(path);
        } catch (Exception e) {
            log.warn("Failed to delete file from storage after DB delete: {}", path, e);
            // Don't throw - file may already not exist, deletion is idempotent
        }
    }

    /**
     * Check if user has permission to access (read/download/preview) a file.
     * Rule: uploadedBy == currentUser OR user has ADMIN/MODERATOR role,
     * OR any {@link StoredFileDownloadAccessPort} allows download for this file and user.
     */
    private void checkAccessPermission(StoredFile entity, UUID currentUserId) {
        if (entity.getUploadedBy() != null && entity.getUploadedBy().equals(currentUserId)) {
            return; // Owner can access
        }
        UserDto user = userApi.findById(currentUserId)
            .orElseThrow(() -> DocumentErrors.accessDenied());
        if (user.hasRole(Role.ADMIN) || user.hasRole(Role.MODERATOR) || user.hasRole(Role.SUPER_ADMIN)) {
            return; // Admin/Moderator can access
        }
        for (StoredFileDownloadAccessPort port : storedFileDownloadAccessPorts) {
            if (port.canDownload(entity.getId(), currentUserId)) {
                return;
            }
        }
        throw DocumentErrors.accessDenied();
    }

    /**
     * Check if user has permission to delete a file.
     * Rule: uploadedBy == currentUser OR user has ADMIN/MODERATOR role.
     */
    private void checkDeletePermission(StoredFile entity, UUID currentUserId) {
        if (entity.getUploadedBy() != null && entity.getUploadedBy().equals(currentUserId)) {
            return; // Owner can delete
        }
        UserDto user = userApi.findById(currentUserId)
            .orElseThrow(() -> DocumentErrors.accessDenied());
        if (user.hasRole(Role.ADMIN) || user.hasRole(Role.MODERATOR) || user.hasRole(Role.SUPER_ADMIN)) {
            return; // Admin/Moderator can delete
        }
        throw DocumentErrors.accessDenied();
    }

    /**
     * Check if file is currently in use (referenced by CourseMaterial, Homework, lesson material, or other business entities).
     * Prevents deletion of files that are still referenced.
     */
    private void checkFileNotInUse(UUID storedFileId) {
        if (courseMaterialRepository.existsByStoredFileId(storedFileId)) {
            throw DocumentErrors.fileInUse();
        }
        if (homeworkFileRepository.existsByStoredFileId(storedFileId)) {
            throw DocumentErrors.fileInUse();
        }
        if (lessonMaterialFileRepository.existsByStoredFileId(storedFileId)) {
            throw DocumentErrors.fileInUse();
        }
        for (StoredFileUsagePort port : storedFileUsagePorts) {
            if (port.isStoredFileInUse(storedFileId)) {
                throw DocumentErrors.fileInUse();
            }
        }
    }

    /**
     * Check if exception indicates storage unavailability (network error, timeout, service down).
     * Checks both the exception itself and its cause (for wrapped exceptions).
     */
    private boolean isStorageUnavailable(Exception e) {
        // Check the exception itself
        if (e instanceof ConnectException
            || e instanceof SocketTimeoutException
            || e instanceof InterruptedIOException) {
            return true;
        }
        
        // Check message
        String message = e.getMessage();
        if (message != null && (
            message.contains("Connection refused")
            || message.contains("timeout")
            || message.contains("unavailable")
            || message.contains("Connection timed out")
        )) {
            return true;
        }
        
        // Check cause (for wrapped exceptions like RuntimeException from MinioStorageAdapter)
        Throwable cause = e.getCause();
        if (cause != null && cause != e) {
            if (cause instanceof ConnectException
                || cause instanceof SocketTimeoutException
                || cause instanceof InterruptedIOException) {
                return true;
            }
            String causeMessage = cause.getMessage();
            if (causeMessage != null && (
                causeMessage.contains("Connection refused")
                || causeMessage.contains("timeout")
                || causeMessage.contains("unavailable")
                || causeMessage.contains("Connection timed out")
            )) {
                return true;
            }
        }
        
        return false;
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
}
