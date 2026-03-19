package com.example.interhubdev.storedfile;

import com.example.interhubdev.error.AppException;

import java.io.InputStream;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Public API for stored file operations: upload, metadata, content, presigned URL, delete.
 * No authorization: caller (e.g. document module) is responsible for access control.
 */
public interface StoredFileApi {

    /**
     * Upload a file: security check, storage, persist metadata. On DB failure after storage upload, file is removed from storage.
     *
     * @param tempFile         path to temp file (caller deletes after)
     * @param originalFilename original file name
     * @param contentType      MIME type
     * @param size             file size in bytes
     * @param uploadedBy       user id (stored as metadata only)
     * @return created metadata
     * @throws AppException e.g. FILE_TOO_LARGE, FORBIDDEN_FILE_TYPE, MALWARE_DETECTED
     */
    StoredFileMeta upload(Path tempFile, String originalFilename, String contentType, long size, UUID uploadedBy);

    /**
     * Upload multiple files. All-or-nothing: on any failure, already-uploaded files in this call are removed.
     *
     * @param inputs    list of file inputs
     * @param uploadedBy user id
     * @return list of created metadata in same order as inputs
     * @throws AppException on validation or storage failure
     */
    List<StoredFileMeta> uploadBatch(List<StoredFileInput> inputs, UUID uploadedBy);

    /**
     * Get metadata by id.
     *
     * @param id stored file id
     * @return metadata if found
     */
    Optional<StoredFileMeta> getMetadata(UUID id);

    /**
     * Get metadata by id or throw NOT_FOUND.
     */
    StoredFileMeta getMetadataOrThrow(UUID id);

    /**
     * Get content stream by id. Caller is responsible for closing the stream. No user/permission check.
     *
     * @param id stored file id
     * @return input stream of file content
     * @throws AppException NOT_FOUND if file not found
     */
    InputStream getContent(UUID id);

    /**
     * Generate presigned URL for download/preview.
     *
     * @param id             stored file id
     * @param expiresSeconds URL expiration in seconds
     * @return presigned URL or empty if not available
     * @throws AppException NOT_FOUND if file not found
     */
    Optional<String> getPresignedUrl(UUID id, int expiresSeconds);

    /**
     * Delete stored file and remove from storage. Fails if {@link StoredFileUsagePort#isStoredFileInUse} returns true.
     *
     * @param id stored file id
     * @throws AppException NOT_FOUND if not found, CONFLICT if file is in use
     */
    void delete(UUID id);

    /**
     * Get entity reference for JPA attach (e.g. document module setting @ManyToOne). Does not load content.
     *
     * @param id stored file id
     * @return entity for reference only
     * @throws AppException NOT_FOUND if not found
     */
    StoredFile getReference(UUID id);
}
