package com.example.interhubdev.document.internal.storedFile;

import com.example.interhubdev.document.StoredFileDto;
import com.example.interhubdev.storedfile.StoredFileMeta;

/**
 * Maps storedfile metadata to document's StoredFileDto. No instantiation.
 */
public final class StoredFileMappers {

    private StoredFileMappers() {
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
