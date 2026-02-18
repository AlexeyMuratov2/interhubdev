package com.example.interhubdev.document.internal.lessonMaterial;

import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

/**
 * Request body for adding files to an existing lesson material.
 */
public record AddLessonMaterialFilesRequest(
    @NotNull(message = "storedFileIds is required")
    List<UUID> storedFileIds
) {
}
