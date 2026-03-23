package com.example.interhubdev.document.internal.homework;

import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

/**
 * Request body for updating homework. All fields optional.
 * New uploads come from multipart file parts. Existing attachment ids can be retained selectively.
 */
public record UpdateHomeworkRequest(
    @Size(max = 500, message = "title must not exceed 500 characters")
    String title,

    @Size(max = 5000)
    String description,

    Integer points,

    Boolean clearAttachments,

    List<UUID> retainAttachmentIds
) {
}
