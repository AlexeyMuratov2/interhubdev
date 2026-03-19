package com.example.interhubdev.storedfile.internal;

import com.example.interhubdev.error.AppException;

import java.io.InputStream;
import java.util.Optional;

/**
 * Port for object storage (S3-compatible). Implemented by MinioStorageAdapter.
 */
public interface StoragePort {

    UploadResult upload(String path, InputStream inputStream, String contentType, long size);

    InputStream download(String path);

    void delete(String path);

    Optional<String> generatePreviewUrl(String path, int expiresInSeconds);

    boolean exists(String path);

    record UploadResult(String path, long size, String contentType) {
    }
}
