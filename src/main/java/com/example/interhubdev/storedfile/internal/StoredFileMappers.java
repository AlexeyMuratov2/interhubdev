package com.example.interhubdev.storedfile.internal;

import com.example.interhubdev.storedfile.FileStatus;
import com.example.interhubdev.storedfile.StoredFile;
import com.example.interhubdev.storedfile.StoredFileMeta;

/**
 * Entity to DTO mapping for stored files.
 */
public final class StoredFileMappers {

    private StoredFileMappers() {
    }

    public static StoredFileMeta toMeta(StoredFile e) {
        return new StoredFileMeta(
            e.getId(),
            e.getSize(),
            e.getContentType(),
            e.getOriginalName(),
            e.getUploadedAt(),
            e.getUploadedBy(),
            e.getStatus() != null ? e.getStatus() : FileStatus.ACTIVE,
            e.getSafetyClass()
        );
    }
}
