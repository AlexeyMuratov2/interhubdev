package com.example.interhubdev.document.internal.storedFile;

import com.example.interhubdev.document.StoredFileDto;

/**
 * Entity to DTO mapping for stored files. No instantiation.
 */
public final class StoredFileMappers {

    private StoredFileMappers() {
    }

    public static StoredFileDto toDto(StoredFile e) {
        return new StoredFileDto(
            e.getId(),
            e.getSize(),
            e.getContentType(),
            e.getOriginalName(),
            e.getUploadedAt(),
            e.getUploadedBy()
        );
    }
}
