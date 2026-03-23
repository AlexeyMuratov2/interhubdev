package com.example.interhubdev.submission;

import com.example.interhubdev.fileasset.FileAssetDownloadHandle;

import java.util.Optional;
import java.util.UUID;

/**
 * Public API for status lookup and controlled download of submission attachments.
 */
public interface SubmissionAttachmentApi {

    Optional<SubmissionAttachmentDto> get(UUID attachmentId, UUID requesterId);

    FileAssetDownloadHandle download(UUID attachmentId, UUID requesterId);
}
