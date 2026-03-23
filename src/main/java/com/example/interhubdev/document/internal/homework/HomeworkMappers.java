package com.example.interhubdev.document.internal.homework;

import com.example.interhubdev.document.DocumentAttachmentDto;
import com.example.interhubdev.document.HomeworkDto;

import java.util.List;
import java.util.UUID;

/**
 * Entity to DTO mapping for homework. No instantiation.
 */
final class HomeworkMappers {

    private HomeworkMappers() {
    }

    static HomeworkDto toDto(Homework entity, List<DocumentAttachmentDto> attachments) {
        UUID lessonId = entity.getLessonHomework() != null
            ? entity.getLessonHomework().getLessonId()
            : null;
        return new HomeworkDto(
            entity.getId(),
            lessonId,
            entity.getTitle(),
            entity.getDescription(),
            entity.getPoints(),
            attachments != null ? attachments : List.of(),
            entity.getCreatedAt(),
            entity.getUpdatedAt()
        );
    }
}
