package com.example.interhubdev.submission.internal;

import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

/**
 * Request body for creating a homework submission. Files are optional.
 */
public record CreateSubmissionRequest(
    @Size(max = 5000, message = "description must not exceed 5000 characters")
    String description,

    List<UUID> storedFileIds
) {
}
