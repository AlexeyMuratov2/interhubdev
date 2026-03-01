package com.example.interhubdev.absencenotice;

import jakarta.validation.constraints.Size;

/**
 * Request DTO for teacher responding to an absence notice (approve or reject).
 */
public record RespondToAbsenceNoticeRequest(
        @Size(max = 2000, message = "comment must not exceed 2000 characters")
        String comment
) {
}
