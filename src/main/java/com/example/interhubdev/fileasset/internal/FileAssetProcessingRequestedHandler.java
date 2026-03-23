package com.example.interhubdev.fileasset.internal;

import com.example.interhubdev.outbox.OutboxEvent;
import com.example.interhubdev.outbox.OutboxEventHandler;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Background handler for file asset processing requests.
 */
@Component
class FileAssetProcessingRequestedHandler implements OutboxEventHandler {

    private final FileAssetServiceImpl fileAssetService;

    FileAssetProcessingRequestedHandler(FileAssetServiceImpl fileAssetService) {
        this.fileAssetService = fileAssetService;
    }

    @Override
    public String eventType() {
        return FileAssetEventTypes.PROCESSING_REQUESTED;
    }

    @Override
    public void handle(OutboxEvent event) {
        Object rawId = event.getPayload().get("fileAssetId");
        if (rawId == null) {
            throw new IllegalArgumentException("fileAssetId is required");
        }
        fileAssetService.handleProcessingRequested(UUID.fromString(rawId.toString()));
    }
}
