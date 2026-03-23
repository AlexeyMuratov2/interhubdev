package com.example.interhubdev.fileasset.internal;

import io.minio.MinioClient;
import io.minio.GetObjectArgs;
import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.StatObjectArgs;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.Map;

/**
 * Best-effort storage cleanup for fileasset keys stored in MinIO/S3-compatible storage.
 */
@Component
@RequiredArgsConstructor
@Slf4j
class MinioFileAssetStorageAdapter implements FileAssetStoragePort {

    private final MinioClient minioClient;

    @Value("${app.storage.bucket-name:documents}")
    private String bucketName;

    @Override
    public boolean exists(String objectKey) {
        if (objectKey == null || objectKey.isBlank()) {
            return false;
        }
        try {
            minioClient.statObject(
                StatObjectArgs.builder()
                    .bucket(bucketName)
                    .object(objectKey)
                    .build()
            );
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public InputStream openStream(String objectKey) {
        try {
            return minioClient.getObject(
                GetObjectArgs.builder()
                    .bucket(bucketName)
                    .object(objectKey)
                    .build()
            );
        } catch (Exception e) {
            throw new IllegalStateException("Could not open fileasset object " + objectKey, e);
        }
    }

    @Override
    public void promoteToFinal(String sourceObjectKey, String targetObjectKey, long sizeBytes, HardenedObjectMetadata metadata) {
        try (InputStream inputStream = openStream(sourceObjectKey)) {
            ensureBucketExists();
            minioClient.putObject(
                PutObjectArgs.builder()
                    .bucket(bucketName)
                    .object(targetObjectKey)
                    .stream(inputStream, sizeBytes, -1)
                    .contentType(metadata.contentType())
                    .headers(Map.of("Content-Disposition", metadata.contentDisposition()))
                    .build()
            );
            deleteQuietly(sourceObjectKey);
        } catch (Exception e) {
            throw new IllegalStateException("Could not promote fileasset object to final storage", e);
        }
    }

    @Override
    public void deleteQuietly(String objectKey) {
        if (objectKey == null || objectKey.isBlank()) {
            return;
        }
        try {
            minioClient.removeObject(
                RemoveObjectArgs.builder()
                    .bucket(bucketName)
                    .object(objectKey)
                    .build()
            );
        } catch (Exception e) {
            log.debug("Fileasset cleanup could not delete object {}", objectKey, e);
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
            }
        } catch (Exception e) {
            throw new IllegalStateException("Could not ensure fileasset bucket exists", e);
        }
    }
}
