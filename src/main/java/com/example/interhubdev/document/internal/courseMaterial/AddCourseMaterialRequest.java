package com.example.interhubdev.document.internal.courseMaterial;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/**
 * Request body for attaching an already-uploaded stored file to a subject as a course material.
 */
public record AddCourseMaterialRequest(
    @NotNull(message = "storedFileId is required")
    UUID storedFileId,

    @NotBlank(message = "title is required")
    @Size(max = 500, message = "title must not exceed 500 characters")
    String title,

    @Size(max = 2000)
    String description
) {
}
