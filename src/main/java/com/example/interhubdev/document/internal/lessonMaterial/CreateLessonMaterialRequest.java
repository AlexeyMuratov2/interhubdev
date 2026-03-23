package com.example.interhubdev.document.internal.lessonMaterial;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

/**
 * Request body for creating a lesson material (lesson is in path).
 */
public record CreateLessonMaterialRequest(
    @NotBlank(message = "name is required")
    @Size(max = 500, message = "name must not exceed 500 characters")
    String name,

    @Size(max = 5000)
    String description,

    @NotNull(message = "publishedAt is required")
    LocalDateTime publishedAt
) {
}
