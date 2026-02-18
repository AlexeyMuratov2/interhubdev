package com.example.interhubdev.document.internal.homework;

import com.example.interhubdev.document.HomeworkDto;
import com.example.interhubdev.document.StoredFileDto;
import com.example.interhubdev.document.internal.storedFile.StoredFile;
import com.example.interhubdev.document.internal.storedFile.StoredFileMappers;

import java.util.Optional;
import java.util.UUID;

/**
 * Entity to DTO mapping for homework. No instantiation.
 */
final class HomeworkMappers {

    private HomeworkMappers() {
    }

    static HomeworkDto toDto(Homework entity) {
        Optional<StoredFileDto> file = Optional.ofNullable(entity.getStoredFile())
            .map(StoredFileMappers::toDto);
        UUID lessonId = entity.getLessonHomework() != null 
            ? entity.getLessonHomework().getLessonId() 
            : null;
        return new HomeworkDto(
            entity.getId(),
            lessonId,
            entity.getTitle(),
            entity.getDescription(),
            entity.getPoints(),
            file,
            entity.getCreatedAt(),
            entity.getUpdatedAt()
        );
    }
}
