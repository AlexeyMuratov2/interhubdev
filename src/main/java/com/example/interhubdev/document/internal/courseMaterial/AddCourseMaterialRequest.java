package com.example.interhubdev.document.internal.courseMaterial;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request body for creating a course material with an uploaded file.
 */
public record AddCourseMaterialRequest(
    @NotBlank(message = "title is required")
    @Size(max = 500, message = "title must not exceed 500 characters")
    String title,

    @Size(max = 2000)
    String description
) {
}
