package com.example.interhubdev.document;

import com.example.interhubdev.error.AppException;

import java.io.InputStream;
import java.util.Optional;

/**
 * Port for file storage operations. S3-compatible interface.
 * Implemented by MinioStorageAdapter (can be replaced with AWS S3 adapter later).
 * 
 * <p>All methods throw {@link AppException} on failure, which is handled by global exception handler.</p>
 */
public interface StoragePort {
    
    /**
     * Upload file to storage.
     * 
     * @param path storage path (e.g., "documents/{type}/{year}/{month}/{uuid}/{filename}")
     * @param inputStream file content
     * @param contentType MIME type
     * @param size file size in bytes
     * @return upload result with path and metadata
     * @throws AppException if upload fails
     */
    UploadResult upload(String path, InputStream inputStream, String contentType, long size);
    
    /**
     * Download file from storage.
     * 
     * @param path storage path
     * @return file content stream (caller is responsible for closing)
     * @throws AppException if file not found or download fails
     */
    InputStream download(String path);
    
    /**
     * Delete file from storage.
     * 
     * @param path storage path
     * @throws AppException if deletion fails (file may not exist)
     */
    void delete(String path);
    
    /**
     * Generate preview URL (presigned URL for S3/MinIO).
     * 
     * @param path storage path
     * @param expiresInSeconds URL expiration time
     * @return presigned URL or empty if preview not available
     */
    Optional<String> generatePreviewUrl(String path, int expiresInSeconds);
    
    /**
     * Check if file exists.
     * 
     * @param path storage path
     * @return true if exists
     */
    boolean exists(String path);
}
