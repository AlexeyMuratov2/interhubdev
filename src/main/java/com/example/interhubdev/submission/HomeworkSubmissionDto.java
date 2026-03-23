package com.example.interhubdev.submission;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * DTO for a homework submission (student solution).
 * Files are optional; submission can have zero or more attached files.
 */
public record HomeworkSubmissionDto(
    UUID id,
    UUID homeworkId,
    UUID authorId,
    LocalDateTime submittedAt,
    Optional<String> description,
    List<SubmissionAttachmentDto> attachments
) {
}
