package com.example.interhubdev.document.internal.lessonMaterial;

import com.example.interhubdev.document.DocumentAttachmentDto;
import com.example.interhubdev.document.LessonMaterialDto;

import java.util.List;

/**
 * Entity to DTO mapping for lesson materials. No instantiation.
 */
final class LessonMaterialMappers {

    private LessonMaterialMappers() {
    }

    static LessonMaterialDto toDto(LessonMaterial entity, List<DocumentAttachmentDto> attachments) {
        return new LessonMaterialDto(
            entity.getId(),
            entity.getLessonId(),
            entity.getName(),
            entity.getDescription(),
            entity.getAuthorId(),
            entity.getPublishedAt(),
            attachments != null ? attachments : List.of()
        );
    }
}
