package com.example.interhubdev.storedfile;

import com.example.interhubdev.error.AppException;

import java.io.InputStream;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Public API for stored file operations: upload, metadata, content, presigned URL, delete.
 * No authorization: caller (e.g. document module) is responsible for access control.
 * Only ACTIVE files are available for getContent/getPresignedUrl (activation gate). Delivery is constrained by FileSafetyClass and DeliveryContext.
 */
public interface StoredFileApi {

    /**
     * Upload a file: acceptance check, classification, storage, persist with ACTIVE and safety class.
     *
     * @param tempFile         path to temp file (caller deletes after)
     * @param originalFilename original file name
     * @param contentType      MIME type
     * @param size             file size in bytes
     * @param uploadedBy       user id (stored as metadata only)
     * @param contextKey       upload context key for policy selection (acceptance, classification)
     * @return created metadata (status ACTIVE)
     * @throws AppException e.g. FILE_TOO_LARGE, FORBIDDEN_FILE_TYPE, MALWARE_DETECTED
     */
    StoredFileMeta upload(Path tempFile, String originalFilename, String contentType, long size, UUID uploadedBy, UploadContextKey contextKey);

    /**
     * Upload multiple files. All-or-nothing: on any failure, already-uploaded files in this call are removed.
     *
     * @param inputs    list of file inputs
     * @param uploadedBy user id
     * @param contextKey upload context key for policy selection
     * @return list of created metadata in same order as inputs (all ACTIVE)
     * @throws AppException on validation or storage failure
     */
    List<StoredFileMeta> uploadBatch(List<StoredFileInput> inputs, UUID uploadedBy, UploadContextKey contextKey);

    /**
     * Get metadata by id. DELETED files are not returned (empty).
     *
     * @param id stored file id
     * @return metadata if found and not DELETED
     */
    Optional<StoredFileMeta> getMetadata(UUID id);

    /**
     * Get metadata by id or throw NOT_FOUND. Throws if file not found or DELETED.
     */
    StoredFileMeta getMetadataOrThrow(UUID id);

    /**
     * Get metadata for multiple ids. Returns only found entries; missing ids are omitted.
     *
     * @param ids set of stored file ids
     * @return map id -> metadata for each found file
     */
    Map<UUID, StoredFileMeta> getMetadataBatch(Set<UUID> ids);

    /**
     * Get content stream by id for delivery. Activation gate: only ACTIVE files. Delivery policy: file's safety class must allow the given context. Caller is responsible for closing the stream.
     *
     * @param id              stored file id
     * @param deliveryContext delivery context (e.g. ATTACHMENT_ONLY)
     * @return input stream of file content
     * @throws AppException NOT_FOUND if file not found or DELETED, CONFLICT if not ACTIVE, FORBIDDEN if delivery not allowed
     */
    InputStream getContent(UUID id, DeliveryContext deliveryContext);

    /**
     * Generate presigned URL for download. Same activation gate and delivery policy as getContent. Use only when response governance (attachment, safe headers) is guaranteed.
     *
     * @param id              stored file id
     * @param expiresSeconds  URL expiration in seconds
     * @param deliveryContext delivery context (e.g. ATTACHMENT_ONLY)
     * @return presigned URL or empty if not available
     * @throws AppException NOT_FOUND if file not found or DELETED, CONFLICT if not ACTIVE, FORBIDDEN if delivery not allowed
     */
    Optional<String> getPresignedUrl(UUID id, int expiresSeconds, DeliveryContext deliveryContext);

    /**
     * Delete stored file: set status DELETED (terminal), remove from storage. Only via this API; deletion by raw storage key is forbidden. Fails if {@link StoredFileUsagePort#isStoredFileInUse} returns true.
     *
     * @param id stored file id
     * @throws AppException NOT_FOUND if not found or already DELETED, CONFLICT if file is in use
     */
    void delete(UUID id);
}
