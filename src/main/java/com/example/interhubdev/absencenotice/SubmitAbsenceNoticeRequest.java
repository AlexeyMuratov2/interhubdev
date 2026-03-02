package com.example.interhubdev.absencenotice;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

/**
 * Request DTO for submitting or updating an absence notice. One notice can cover multiple lessons.
 */
public record SubmitAbsenceNoticeRequest(
        @NotEmpty(message = "lessonSessionIds is required and must not be empty")
        @Size(min = 1, max = 50, message = "lessonSessionIds must have between 1 and 50 items")
        List<@NotNull(message = "lessonSessionId cannot be null") UUID> lessonSessionIds,

        @NotNull(message = "type is required")
        AbsenceNoticeType type,

        @Size(max = 2000, message = "reasonText must not exceed 2000 characters")
        String reasonText,

        @Size(max = 10, message = "fileIds must not exceed 10 items")
        List<@jakarta.validation.constraints.NotBlank(message = "fileId cannot be blank") String> fileIds
) {
}
