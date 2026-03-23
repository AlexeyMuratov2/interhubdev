package com.example.interhubdev.fileasset;

/**
 * Opaque confirmation that file bytes reached temporary storage.
 * <p>
 * This type intentionally avoids exposing storage coordinates.
 */
public record FileUploadReceipt(
    String uploadToken,
    String checksum,
    String etag
) {
}
