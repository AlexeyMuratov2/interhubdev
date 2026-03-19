package com.example.interhubdev.document.internal.storedFile;

import com.example.interhubdev.document.StoredFileDto;
import com.example.interhubdev.storedfile.StoredFile;
import com.example.interhubdev.storedfile.StoredFileMeta;

/**
 * Maps storedfile entity/metadata to document's StoredFileDto. No instantiation.
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

    public static StoredFileDto fromMeta(StoredFileMeta m) {
        return new StoredFileDto(
            m.id(),
            m.size(),
            m.contentType(),
            m.originalName(),
            m.uploadedAt(),
            m.uploadedBy()
        );
    }
}
