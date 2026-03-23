package com.example.interhubdev.submission.internal;

import com.example.interhubdev.submission.HomeworkSubmissionDto;
import com.example.interhubdev.submission.SubmissionAttachmentDto;

import java.util.List;
import java.util.Optional;

/**
 * Entity to DTO mapping for submissions. No instantiation.
 */
final class SubmissionMappers {

    private SubmissionMappers() {
    }

    static HomeworkSubmissionDto toDto(HomeworkSubmission entity, List<SubmissionAttachmentDto> attachments) {
        return new HomeworkSubmissionDto(
            entity.getId(),
            entity.getHomeworkId(),
            entity.getAuthorId(),
            entity.getSubmittedAt(),
            Optional.ofNullable(entity.getDescription()).filter(d -> !d.isBlank()),
            attachments != null ? attachments : List.of()
        );
    }
}
