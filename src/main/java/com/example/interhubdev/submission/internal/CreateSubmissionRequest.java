package com.example.interhubdev.submission.internal;

import jakarta.validation.constraints.Size;

/**
 * Request body for creating a homework submission. Files are optional.
 */
public record CreateSubmissionRequest(
    @Size(max = 5000, message = "description must not exceed 5000 characters")
    String description
) {
}
