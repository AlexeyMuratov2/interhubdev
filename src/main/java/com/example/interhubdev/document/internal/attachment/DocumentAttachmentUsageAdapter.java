package com.example.interhubdev.document.internal.attachment;

import com.example.interhubdev.fileasset.FileAssetUsagePort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Lightweight adapter used by fileasset to check whether a file asset is still
 * referenced by document business bindings. It depends only on repositories to
 * avoid cycles with the document attachment orchestration service.
 */
@Component
@RequiredArgsConstructor
class DocumentAttachmentUsageAdapter implements FileAssetUsagePort {

    private final DocumentAttachmentRepository documentAttachmentRepository;

    @Override
    public boolean isFileAssetInUse(UUID fileAssetId) {
        return documentAttachmentRepository.countByFileAssetId(fileAssetId) > 0;
    }
}
