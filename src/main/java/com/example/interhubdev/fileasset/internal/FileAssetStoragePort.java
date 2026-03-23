package com.example.interhubdev.fileasset.internal;

import java.io.InputStream;

/**
 * Internal storage contract for lifecycle cleanup, scan access and hardened finalization.
 */
interface FileAssetStoragePort {

    void uploadToTemp(String objectKey, InputStream inputStream, long sizeBytes, HardenedObjectMetadata metadata);

    boolean exists(String objectKey);

    InputStream openStream(String objectKey);

    void promoteToFinal(String sourceObjectKey, String targetObjectKey, long sizeBytes, HardenedObjectMetadata metadata);

    void deleteQuietly(String objectKey);

    record HardenedObjectMetadata(String contentType, String contentDisposition) {
    }
}
