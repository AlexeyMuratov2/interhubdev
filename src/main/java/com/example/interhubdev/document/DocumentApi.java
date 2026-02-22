package com.example.interhubdev.document;

import com.example.interhubdev.error.AppException;

import java.io.InputStream;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Public API for document module: file upload, download, preview, and delete.
 * All operations work on stored files (metadata in DB, content in S3).
 * Errors are thrown as {@link AppException} and handled by global exception handler.
 */
public interface DocumentApi {

    /**
     * Upload a file to storage and persist metadata. Atomic: on DB failure after S3 upload,
     * the file is removed from S3 and exception is thrown.
     *
     * <p>Caller must provide a temp file path; content is read for security scan (antivirus, magic bytes)
     * and upload. Caller is responsible for deleting the temp file after the call.
     *
     * @param tempFile        path to temp file (must exist, readable; caller deletes after)
     * @param originalFilename original file name
     * @param contentType     MIME type
     * @param size            file size in bytes
     * @param uploadedBy      user id of uploader
     * @return created stored file DTO
     * @throws AppException e.g. FILE_TOO_LARGE, FORBIDDEN_FILE_TYPE, MALWARE_DETECTED, etc.
     */
    StoredFileDto uploadFile(Path tempFile, String originalFilename, String contentType, long size, UUID uploadedBy);

    /**
     * Upload multiple files to storage and persist metadata. All-or-nothing: if any file fails
     * (security, validation, storage, or DB), all already-uploaded files in this call are removed
     * from storage and DB, then the exception is rethrown.
     *
     * @param inputs    list of file inputs (temp path, filename, contentType, size); caller deletes temp files after
     * @param uploadedBy user id of uploader
     * @return list of created stored file DTOs in the same order as inputs
     * @throws AppException e.g. BAD_REQUEST if list empty or exceeds max-files-per-batch,
     *                      FILE_TOO_LARGE, FORBIDDEN_FILE_TYPE, MALWARE_DETECTED, etc.
     */
    List<StoredFileDto> uploadFiles(List<FileUploadInput> inputs, UUID uploadedBy);

    /**
     * Get stored file metadata by id.
     *
     * @param id stored file id
     * @return optional DTO if found
     */
    Optional<StoredFileDto> getStoredFile(UUID id);

    /**
     * Download file content by stored file id. Caller is responsible for closing the stream.
     * Requires permission: file owner or user with admin/moderator role.
     *
     * @param id stored file id
     * @param currentUserId current authenticated user id (for permission check)
     * @return input stream of file content
     * @throws AppException NOT_FOUND if stored file not found, FORBIDDEN if access denied
     */
    InputStream downloadByStoredFileId(UUID id, UUID currentUserId);

    /**
     * Generate presigned URL for preview/download.
     * Requires permission: file owner or user with admin/moderator role.
     *
     * @param storedFileId    stored file id
     * @param expiresSeconds  URL expiration in seconds
     * @param currentUserId   current authenticated user id (for permission check)
     * @return optional URL if file exists and URL could be generated
     * @throws AppException NOT_FOUND if stored file not found, FORBIDDEN if access denied
     */
    Optional<String> getPreviewUrl(UUID storedFileId, int expiresSeconds, UUID currentUserId);

    /**
     * Generate presigned URL for direct download (alternative to streaming through backend).
     * Requires permission: file owner or user with admin/moderator role.
     *
     * @param storedFileId    stored file id
     * @param expiresSeconds  URL expiration in seconds
     * @param currentUserId   current authenticated user id (for permission check)
     * @return optional URL if file exists and URL could be generated
     * @throws AppException NOT_FOUND if stored file not found, FORBIDDEN if access denied
     */
    Optional<String> getDownloadUrl(UUID storedFileId, int expiresSeconds, UUID currentUserId);

    /**
     * Delete stored file record and remove file from storage.
     * Requires permission: file owner or user with admin/moderator role.
     * Cannot delete if file is in use (referenced by Document entity).
     *
     * @param id stored file id
     * @param currentUserId current authenticated user id (for permission check)
     * @throws AppException NOT_FOUND if stored file not found, FORBIDDEN if access denied,
     *                      CONFLICT if file is in use
     */
    void deleteStoredFile(UUID id, UUID currentUserId);
}
