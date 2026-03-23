package com.example.interhubdev.fileasset;

import java.nio.file.Path;
import java.util.UUID;

/**
 * Business-facing upload command for ingesting a file into the fileasset lifecycle.
 */
public record FileAssetUploadCommand(
    Path tempFile,
    String originalName,
    String declaredContentType,
    long sizeBytes,
    UUID uploadedBy,
    FilePolicyKey policyKey
) {
}
