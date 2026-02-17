package com.example.interhubdev.document.internal.homework;

import jakarta.validation.constraints.Size;

import java.util.UUID;

/**
 * Request body for updating homework. All fields optional.
 * Use clearFile=true to remove file reference (file in storage is not deleted).
 * Use storedFileId (when clearFile=false) to set or change the file.
 */
public record UpdateHomeworkRequest(
    @Size(max = 500, message = "title must not exceed 500 characters")
    String title,

    @Size(max = 5000)
    String description,

    Integer points,

    /** If true, clear the file reference. Ignored if storedFileId is set. */
    Boolean clearFile,

    /** New stored file id. Used only when clearFile is not true. */
    UUID storedFileId
) {
}
