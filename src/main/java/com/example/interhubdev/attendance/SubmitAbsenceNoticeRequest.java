package com.example.interhubdev.attendance;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

/**
 * Request DTO for submitting or updating an absence notice.
 */
public record SubmitAbsenceNoticeRequest(
        @NotNull(message = "lessonSessionId is required")
        UUID lessonSessionId,

        @NotNull(message = "type is required")
        AbsenceNoticeType type,

        @Size(max = 2000, message = "reasonText must not exceed 2000 characters")
        String reasonText,

        @Size(max = 10, message = "fileIds must not exceed 10 items")
        List<@jakarta.validation.constraints.NotBlank(message = "fileId cannot be blank") String> fileIds
) {
}
