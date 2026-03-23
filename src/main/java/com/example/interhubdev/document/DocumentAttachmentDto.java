package com.example.interhubdev.document;

import com.example.interhubdev.fileasset.FileAssetStage;
import com.example.interhubdev.fileasset.FileAssetStatus;

import java.util.UUID;

/**
 * Safe business-facing projection of a document attachment.
 */
public record DocumentAttachmentDto(
    UUID id,
    String fileName,
    String declaredContentType,
    long sizeBytes,
    FileAssetStatus status,
    FileAssetStage stage,
    int progressPercent,
    String failureCode,
    boolean downloadAvailable
) {
}
