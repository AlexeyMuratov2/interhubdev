package com.example.interhubdev.document.internal.lessonMaterial;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

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
    LocalDateTime publishedAt,

    List<UUID> storedFileIds
) {
    /**
     * Returns stored file IDs; empty list if null.
     */
    public List<UUID> storedFileIds() {
        return storedFileIds != null ? storedFileIds : List.of();
    }
}
