package com.example.interhubdev.submission.internal;

import com.example.interhubdev.submission.HomeworkSubmissionDto;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Entity to DTO mapping for submissions. No instantiation.
 */
final class SubmissionMappers {

    private SubmissionMappers() {
    }

    static HomeworkSubmissionDto toDto(HomeworkSubmission entity, List<UUID> storedFileIdsInOrder) {
        return new HomeworkSubmissionDto(
            entity.getId(),
            entity.getHomeworkId(),
            entity.getAuthorId(),
            entity.getSubmittedAt(),
            Optional.ofNullable(entity.getDescription()).filter(d -> !d.isBlank()),
            storedFileIdsInOrder != null ? storedFileIdsInOrder : List.of()
        );
    }
}
