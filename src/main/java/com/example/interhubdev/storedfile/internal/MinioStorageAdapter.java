package com.example.interhubdev.storedfile.internal;

import com.example.interhubdev.error.AppException;
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
 */
@Component
@RequiredArgsConstructor
@Slf4j
class MinioStorageAdapter implements StoragePort {

    private final MinioClient minioClient;

    @Value("${app.storage.bucket-name}")
    private String bucketName;

    @Override
    public UploadResult upload(String path, InputStream inputStream, String contentType, long size) {
        try {
            ensureBucketExists();
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
            if (e instanceof AppException ex) {
                throw ex;
            }
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
            throw StoredFileErrors.fileNotFoundInStorage();
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
        }
    }

    @Override
    public Optional<String> generatePreviewUrl(String path, int expiresInSeconds) {
        try {
            if (!exists(path)) {
                return Optional.empty();
            }
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
            throw new RuntimeException("Storage bucket access failed", e);
        }
    }
}
