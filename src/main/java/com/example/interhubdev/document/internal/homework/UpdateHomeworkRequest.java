package com.example.interhubdev.document.internal.homework;

import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

/**
 * Request body for updating homework. All fields optional.
 * Use clearFiles=true to remove all file links (files in storage are not deleted).
 * Use storedFileIds (when clearFiles is not true) to replace the file list; order preserved.
 */
public record UpdateHomeworkRequest(
    @Size(max = 500, message = "title must not exceed 500 characters")
    String title,

    @Size(max = 5000)
    String description,

    Integer points,

    /** If true, clear all file links. Ignored if storedFileIds is provided. */
    Boolean clearFiles,

    /** New list of stored file ids (full replacement). Used only when clearFiles is not true; null = no change. */
    List<UUID> storedFileIds
) {
}
