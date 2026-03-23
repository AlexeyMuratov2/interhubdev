package com.example.interhubdev.document.internal.homework;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request body for creating homework (lesson is in path).
 */
public record CreateHomeworkRequest(
    @NotBlank(message = "title is required")
    @Size(max = 500, message = "title must not exceed 500 characters")
    String title,

    @Size(max = 5000)
    String description,

    Integer points
) {
}
