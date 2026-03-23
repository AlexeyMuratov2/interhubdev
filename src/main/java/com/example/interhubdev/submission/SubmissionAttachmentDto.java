package com.example.interhubdev.submission;

import com.example.interhubdev.fileasset.FileAssetStage;
import com.example.interhubdev.fileasset.FileAssetStatus;

import java.util.UUID;

/**
 * Safe business-facing projection of a submission attachment.
 */
public record SubmissionAttachmentDto(
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
