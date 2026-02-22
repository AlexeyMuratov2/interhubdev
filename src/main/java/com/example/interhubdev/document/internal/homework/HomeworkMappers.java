package com.example.interhubdev.document.internal.homework;

import com.example.interhubdev.document.HomeworkDto;
import com.example.interhubdev.document.StoredFileDto;
import com.example.interhubdev.document.internal.storedFile.StoredFileMappers;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Entity to DTO mapping for homework. No instantiation.
 */
final class HomeworkMappers {

    private HomeworkMappers() {
    }

    static HomeworkDto toDto(Homework entity) {
        List<StoredFileDto> files = entity.getFiles().stream()
            .map(HomeworkFile::getStoredFile)
            .filter(Objects::nonNull)
            .map(StoredFileMappers::toDto)
            .toList();
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
