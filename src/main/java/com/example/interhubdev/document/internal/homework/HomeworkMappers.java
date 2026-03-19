package com.example.interhubdev.document.internal.homework;

import com.example.interhubdev.document.HomeworkDto;
import com.example.interhubdev.document.StoredFileDto;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Entity to DTO mapping for homework. No instantiation.
 */
final class HomeworkMappers {

    private HomeworkMappers() {
    }

    /**
     * Maps entity to DTO using pre-loaded stored file metadata.
     *
     * @param entity   homework entity
     * @param filesMap map of storedFileId -> StoredFileDto (must contain all file ids from entity.getFiles())
     */
    static HomeworkDto toDto(Homework entity, Map<UUID, StoredFileDto> filesMap) {
        List<StoredFileDto> files = entity.getFiles().stream()
            .map(f -> {
                StoredFileDto dto = filesMap.get(f.getStoredFileId());
                if (dto == null) {
                    throw new IllegalStateException("Stored file metadata missing for id: " + f.getStoredFileId());
                }
                return dto;
            })
            .collect(Collectors.toList());
        UUID lessonId = entity.getLessonHomework() != null
            ? entity.getLessonHomework().getLessonId()
            : null;
        return new HomeworkDto(
            entity.getId(),
            lessonId,
            entity.getTitle(),
            entity.getDescription(),
            entity.getPoints(),
            files,
            entity.getCreatedAt(),
            entity.getUpdatedAt()
        );
    }
}
