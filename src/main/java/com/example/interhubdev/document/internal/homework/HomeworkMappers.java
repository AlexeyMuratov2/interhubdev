package com.example.interhubdev.document.internal.homework;

import com.example.interhubdev.document.HomeworkDto;
import com.example.interhubdev.document.StoredFileDto;
import com.example.interhubdev.document.internal.storedFile.StoredFile;
import com.example.interhubdev.document.internal.storedFile.StoredFileMappers;

import java.util.Optional;

/**
 * Entity to DTO mapping for homework. No instantiation.
 */
final class HomeworkMappers {

    private HomeworkMappers() {
    }

    static HomeworkDto toDto(Homework entity) {
        Optional<StoredFileDto> file = Optional.ofNullable(entity.getStoredFile())
            .map(StoredFileMappers::toDto);
        return new HomeworkDto(
            entity.getId(),
            entity.getLessonId(),
            entity.getTitle(),
            entity.getDescription(),
            entity.getPoints(),
            file,
            entity.getCreatedAt(),
            entity.getUpdatedAt()
        );
    }
}
