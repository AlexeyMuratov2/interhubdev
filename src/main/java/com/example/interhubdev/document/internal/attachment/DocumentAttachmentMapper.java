package com.example.interhubdev.document.internal.attachment;

import com.example.interhubdev.document.DocumentAttachmentDto;
import com.example.interhubdev.fileasset.FileAssetView;

final class DocumentAttachmentMapper {

    private DocumentAttachmentMapper() {
    }

    static DocumentAttachmentDto toDto(DocumentAttachment attachment, FileAssetView asset) {
        if (asset == null) {
            throw new IllegalStateException("File asset metadata missing for document attachment " + attachment.getId());
        }
        return new DocumentAttachmentDto(
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
