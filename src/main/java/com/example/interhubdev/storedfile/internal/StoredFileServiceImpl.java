package com.example.interhubdev.storedfile.internal;

import com.example.interhubdev.error.AppException;
import com.example.interhubdev.error.Errors;
import com.example.interhubdev.storedfile.StoredFile;
import com.example.interhubdev.storedfile.StoredFileApi;
import com.example.interhubdev.storedfile.StoredFileInput;
import com.example.interhubdev.storedfile.StoredFileMeta;
import com.example.interhubdev.storedfile.StoredFileUsagePort;
import com.example.interhubdev.storedfile.internal.uploadSecurity.UploadContext;
import com.example.interhubdev.storedfile.internal.uploadSecurity.UploadSecurityPort;
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
 * Implementation of {@link StoredFileApi}.
 */
@Service
@RequiredArgsConstructor
@Slf4j
class StoredFileServiceImpl implements StoredFileApi {

    private final UploadSecurityPort uploadSecurityPort;
    private final StoragePort storagePort;
    private final StoredFileRepository storedFileRepository;
    private final FileValidation fileValidation;
    private final List<StoredFileUsagePort> storedFileUsagePorts;

    @Value("${app.document.max-files-per-batch:50}")
    private int maxFilesPerBatch;

    @Override
    @Transactional
    public StoredFileMeta upload(Path tempFile, String originalFilename, String contentType, long size, UUID uploadedBy) {
        StoredFileAndPath result = uploadOne(tempFile, originalFilename, contentType, size, uploadedBy);
        return StoredFileMappers.toMeta(result.entity());
    }

    @Override
    @Transactional
    public List<StoredFileMeta> uploadBatch(List<StoredFileInput> inputs, UUID uploadedBy) {
        if (inputs == null || inputs.isEmpty()) {
            throw Errors.badRequest("At least one file is required");
        }
        if (inputs.size() > maxFilesPerBatch) {
            throw StoredFileErrors.batchTooLarge(maxFilesPerBatch);
        }
        List<StoredFileAndPath> uploaded = new ArrayList<>();
        try {
            for (StoredFileInput input : inputs) {
                StoredFileAndPath result = uploadOne(
                    input.tempPath(), input.originalFilename(), input.contentType(), input.size(), uploadedBy);
                uploaded.add(result);
            }
            return uploaded.stream().map(s -> StoredFileMappers.toMeta(s.entity())).toList();
        } catch (Exception e) {
            for (StoredFileAndPath u : uploaded) {
                try {
                    storedFileRepository.deleteById(u.entity().getId());
                } catch (Exception ex) {
                    log.warn("Rollback: delete StoredFile {} failed", u.entity().getId(), ex);
                }
                try {
                    storagePort.delete(u.storagePath());
                } catch (Exception ex) {
                    log.warn("Rollback: delete from storage {} failed", u.storagePath(), ex);
                }
            }
            if (e instanceof AppException ex) {
                throw ex;
            }
            throw e;
        }
    }

    private StoredFileAndPath uploadOne(Path tempFile, String originalFilename, String contentType, long size, UUID uploadedBy) {
        uploadSecurityPort.ensureUploadAllowed(
            UploadContext.of(uploadedBy, contentType, size, originalFilename), tempFile);
        fileValidation.validateUpload(size, contentType, originalFilename);
        String sanitizedName = sanitizeFilename(originalFilename);
        UUID id = UUID.randomUUID();
        YearMonth now = YearMonth.now();
        String path = "files/" + now.getYear() + "/" + now.getMonthValue() + "/" + id + "_" + sanitizedName;

        StoragePort.UploadResult uploadResult;
        try (InputStream stream = Files.newInputStream(tempFile)) {
            uploadResult = storagePort.upload(path, stream, contentType, size);
        } catch (Exception e) {
            if (e instanceof AppException ex) {
                throw ex;
            }
            log.error("Failed to upload file to storage: {}", path, e);
            if (isStorageUnavailable(e)) {
                throw StoredFileErrors.storageUnavailable();
            }
            throw StoredFileErrors.uploadFailed();
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
            log.warn("Failed to save StoredFile after S3 upload, removing from storage: {}", path, e);
            try {
                storagePort.delete(path);
            } catch (Exception ex) {
                log.error("Compensation delete failed: {}", path, ex);
            }
            if (e instanceof AppException ex) {
                throw ex;
            }
            throw StoredFileErrors.saveFailed();
        }
    }

    private record StoredFileAndPath(StoredFile entity, String storagePath) {
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<StoredFileMeta> getMetadata(UUID id) {
        return storedFileRepository.findById(id).map(StoredFileMappers::toMeta);
    }

    @Override
    @Transactional(readOnly = true)
    public StoredFileMeta getMetadataOrThrow(UUID id) {
        return storedFileRepository.findById(id)
            .map(StoredFileMappers::toMeta)
            .orElseThrow(() -> StoredFileErrors.storedFileNotFound(id));
    }

    @Override
    @Transactional(readOnly = true)
    public InputStream getContent(UUID id) {
        StoredFile entity = storedFileRepository.findById(id)
            .orElseThrow(() -> StoredFileErrors.storedFileNotFound(id));
        try {
            return storagePort.download(entity.getStoragePath());
        } catch (Exception e) {
            if (e instanceof AppException ex) {
                throw ex;
            }
            log.error("Failed to download from storage: {}", entity.getStoragePath(), e);
            if (isStorageUnavailable(e)) {
                throw StoredFileErrors.storageUnavailable();
            }
            throw StoredFileErrors.storedFileNotFound(id);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<String> getPresignedUrl(UUID id, int expiresSeconds) {
        StoredFile entity = storedFileRepository.findById(id)
            .orElseThrow(() -> StoredFileErrors.storedFileNotFound(id));
        Optional<String> url = storagePort.generatePreviewUrl(entity.getStoragePath(), expiresSeconds);
        if (url.isEmpty()) {
            throw StoredFileErrors.fileNotFoundInStorage();
        }
        return url;
    }

    @Override
    @Transactional(readOnly = true)
    public StoredFile getReference(UUID id) {
        return storedFileRepository.findById(id)
            .orElseThrow(() -> StoredFileErrors.storedFileNotFound(id));
    }

    @Override
    @Transactional
    public void delete(UUID id) {
        StoredFile entity = storedFileRepository.findById(id)
            .orElseThrow(() -> StoredFileErrors.storedFileNotFound(id));
        for (StoredFileUsagePort port : storedFileUsagePorts) {
            if (port.isStoredFileInUse(id)) {
                throw StoredFileErrors.fileInUse();
            }
        }
        String path = entity.getStoragePath();
        storedFileRepository.delete(entity);
        try {
            storagePort.delete(path);
        } catch (Exception e) {
            log.warn("Failed to delete file from storage after DB delete: {}", path, e);
        }
    }

    private static boolean isStorageUnavailable(Exception e) {
        if (e instanceof ConnectException || e instanceof SocketTimeoutException || e instanceof InterruptedIOException) {
            return true;
        }
        String msg = e.getMessage();
        if (msg != null && (msg.contains("Connection refused") || msg.contains("timeout") || msg.contains("unavailable") || msg.contains("Connection timed out"))) {
            return true;
        }
        Throwable cause = e.getCause();
        if (cause != null && cause != e) {
            if (cause instanceof ConnectException || cause instanceof SocketTimeoutException || cause instanceof InterruptedIOException) {
                return true;
            }
            String cm = cause.getMessage();
            if (cm != null && (cm.contains("Connection refused") || cm.contains("timeout") || cm.contains("unavailable"))) {
                return true;
            }
        }
        return false;
    }

    private static String sanitizeFilename(String name) {
        if (name == null || name.isBlank()) return "file";
        String s = name.replace('\\', '_').replace('/', '_').replace("\0", "");
        if (s.length() > 255) s = s.substring(0, 255);
        return s.isEmpty() ? "file" : s;
    }
}
