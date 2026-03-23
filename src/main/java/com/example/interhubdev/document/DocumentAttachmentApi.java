package com.example.interhubdev.document;

import com.example.interhubdev.fileasset.FileAssetDownloadHandle;

import java.util.Optional;
import java.util.UUID;

/**
 * Public API for status lookup and controlled download of document attachments.
 */
public interface DocumentAttachmentApi {

    Optional<DocumentAttachmentDto> get(UUID attachmentId, UUID requesterId);

    FileAssetDownloadHandle download(UUID attachmentId, UUID requesterId);
}
