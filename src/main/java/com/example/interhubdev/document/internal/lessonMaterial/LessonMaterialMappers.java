package com.example.interhubdev.document.internal.lessonMaterial;

import com.example.interhubdev.document.LessonMaterialDto;
import com.example.interhubdev.document.StoredFileDto;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Entity to DTO mapping for lesson materials. No instantiation.
 */
final class LessonMaterialMappers {

    private LessonMaterialMappers() {
    }

    /**
     * Maps entity to DTO using pre-loaded stored file metadata.
     *
     * @param entity   lesson material entity
     * @param filesMap map of storedFileId -> StoredFileDto (must contain all file ids from entity.getFiles())
     */
    static LessonMaterialDto toDto(LessonMaterial entity, Map<UUID, StoredFileDto> filesMap) {
        List<StoredFileDto> fileDtos = entity.getFiles().stream()
            .map(f -> {
                StoredFileDto dto = filesMap.get(f.getStoredFileId());
                if (dto == null) {
                    throw new IllegalStateException("Stored file metadata missing for id: " + f.getStoredFileId());
                }
                return dto;
            })
            .collect(Collectors.toList());
        return new LessonMaterialDto(
            entity.getId(),
            entity.getLessonId(),
            entity.getName(),
            entity.getDescription(),
            entity.getAuthorId(),
            entity.getPublishedAt(),
            fileDtos
        );
    }
}
