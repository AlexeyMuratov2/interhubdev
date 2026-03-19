package com.example.interhubdev.document.internal.courseMaterial;

import com.example.interhubdev.document.CourseMaterialDto;
import com.example.interhubdev.document.StoredFileDto;

import java.util.Map;
import java.util.UUID;

/**
 * Entity to DTO mapping for course materials. No instantiation.
 */
final class CourseMaterialMappers {

    private CourseMaterialMappers() {
    }

    /**
     * Maps entity to DTO using pre-loaded stored file metadata.
     *
     * @param entity   course material entity
     * @param filesMap map of storedFileId -> StoredFileDto (must contain entity.getStoredFileId())
     */
    static CourseMaterialDto toDto(CourseMaterial entity, Map<UUID, StoredFileDto> filesMap) {
        StoredFileDto fileDto = filesMap.get(entity.getStoredFileId());
        if (fileDto == null) {
            throw new IllegalStateException("Stored file metadata missing for id: " + entity.getStoredFileId());
        }
        return new CourseMaterialDto(
            entity.getId(),
            entity.getOfferingId(),
            entity.getTitle(),
            entity.getDescription(),
            entity.getAuthorId(),
            entity.getUploadedAt(),
            fileDto
        );
    }
}
