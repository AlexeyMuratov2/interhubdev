package com.example.interhubdev.submission.internal;

import com.example.interhubdev.fileasset.FileAssetView;
import com.example.interhubdev.submission.SubmissionAttachmentDto;

final class SubmissionAttachmentMapper {

    private SubmissionAttachmentMapper() {
    }

    static SubmissionAttachmentDto toDto(SubmissionAttachment attachment, FileAssetView asset) {
        if (asset == null) {
            throw new IllegalStateException("File asset metadata missing for submission attachment " + attachment.getId());
        }
        return new SubmissionAttachmentDto(
            attachment.getId(),
            asset.originalName(),
            asset.declaredContentType(),
            asset.sizeBytes(),
            asset.status(),
            asset.stage(),
            asset.progressPercent(),
            asset.lastFailureCode(),
            asset.downloadAvailable()
        );
    }
}
