package com.example.interhubdev.document.internal.storedFile;

import com.example.interhubdev.document.StoragePort;
import com.example.interhubdev.document.UploadResult;
import io.minio.BucketExistsArgs;
import io.minio.GetObjectArgs;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.StatObjectArgs;
import io.minio.http.Method;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.Optional;

/**
 * MinIO implementation of StoragePort.
 * Uses S3-compatible API, so can be easily replaced with AWS S3 adapter.
 */
@Component
@RequiredArgsConstructor
@Slf4j
class MinioStorageAdapter implements StoragePort {
    
    private final MinioClient minioClient;
    
    @Value("${app.storage.bucket-name}")
    private String bucketName;
    
    @Value("${app.storage.preview-url-expires-seconds:3600}")
    private int previewUrlExpiresSeconds;
    
    @Override
    public UploadResult upload(String path, InputStream inputStream, String contentType, long size) {
        try {
            // Ensure bucket exists
            ensureBucketExists();
            
            // Upload file
            minioClient.putObject(
                PutObjectArgs.builder()
                    .bucket(bucketName)
                    .object(path)
                    .stream(inputStream, size, -1)
                    .contentType(contentType)
                    .build()
            );
            
            log.debug("File uploaded successfully: {}", path);
            return new UploadResult(path, size, contentType);
            
        } catch (Exception e) {
            log.error("Failed to upload file to storage: {}", path, e);
            // Don't expose internal error details to client
            // Error will be wrapped in DocumentServiceImpl with appropriate status
            throw new RuntimeException("Storage upload failed", e);
        }
    }
    
    @Override
    public InputStream download(String path) {
        try {
            return minioClient.getObject(
                GetObjectArgs.builder()
                    .bucket(bucketName)
                    .object(path)
                    .build()
            );
        } catch (Exception e) {
            log.error("Failed to download file from storage: {}", path, e);
            throw DocumentErrors.fileNotFoundInStorage();
        }
    }
    
    @Override
    public void delete(String path) {
        try {
            minioClient.removeObject(
                RemoveObjectArgs.builder()
                    .bucket(bucketName)
                    .object(path)
                    .build()
            );
            log.debug("File deleted successfully: {}", path);
        } catch (Exception e) {
            log.warn("Failed to delete file from storage: {}", path, e);
            // Don't throw exception - file may already not exist
        }
    }
    
    @Override
    public Optional<String> generatePreviewUrl(String path, int expiresInSeconds) {
        try {
            // Check if file exists first
            if (!exists(path)) {
                return Optional.empty();
            }
            
            // Generate presigned URL for preview
            String url = minioClient.getPresignedObjectUrl(
                GetPresignedObjectUrlArgs.builder()
                    .method(Method.GET)
                    .bucket(bucketName)
                    .object(path)
                    .expiry(expiresInSeconds)
                    .build()
            );
            return Optional.of(url);
        } catch (Exception e) {
            log.warn("Failed to generate preview URL for: {}", path, e);
            return Optional.empty();
        }
    }
    
    @Override
    public boolean exists(String path) {
        try {
            minioClient.statObject(
                StatObjectArgs.builder()
                    .bucket(bucketName)
                    .object(path)
                    .build()
            );
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Ensure bucket exists, create if it doesn't.
     */
    private void ensureBucketExists() {
        try {
            boolean exists = minioClient.bucketExists(
                BucketExistsArgs.builder()
                    .bucket(bucketName)
                    .build()
            );
            
            if (!exists) {
                minioClient.makeBucket(
                    MakeBucketArgs.builder()
                        .bucket(bucketName)
                        .build()
                );
                log.info("Created MinIO bucket: {}", bucketName);
            }
        } catch (Exception e) {
            log.error("Failed to check/create bucket: {}", bucketName, e);
            // Don't expose internal error details to client
            // Error will be wrapped in DocumentServiceImpl with appropriate status
            throw new RuntimeException("Storage bucket access failed", e);
        }
    }
}
