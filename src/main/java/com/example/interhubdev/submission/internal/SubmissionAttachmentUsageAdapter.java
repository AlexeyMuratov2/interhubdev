package com.example.interhubdev.submission.internal;

import com.example.interhubdev.fileasset.FileAssetUsagePort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Lightweight repository-backed adapter used by fileasset to check whether a
 * file asset is still referenced by submission attachments.
 */
@Component
@RequiredArgsConstructor
class SubmissionAttachmentUsageAdapter implements FileAssetUsagePort {

    private final SubmissionAttachmentRepository submissionAttachmentRepository;

    @Override
    public boolean isFileAssetInUse(UUID fileAssetId) {
        return submissionAttachmentRepository.countByFileAssetId(fileAssetId) > 0;
    }
}
