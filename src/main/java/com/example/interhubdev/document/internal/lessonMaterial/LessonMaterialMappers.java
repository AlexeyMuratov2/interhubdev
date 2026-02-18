package com.example.interhubdev.document.internal.lessonMaterial;

import com.example.interhubdev.document.LessonMaterialDto;
import com.example.interhubdev.document.StoredFileDto;
import com.example.interhubdev.document.internal.storedFile.StoredFileMappers;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Entity to DTO mapping for lesson materials. No instantiation.
 */
final class LessonMaterialMappers {

    private LessonMaterialMappers() {
    }

    static LessonMaterialDto toDto(LessonMaterial entity) {
        List<StoredFileDto> fileDtos = entity.getFiles().stream()
            .map(f -> StoredFileMappers.toDto(f.getStoredFile()))
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
