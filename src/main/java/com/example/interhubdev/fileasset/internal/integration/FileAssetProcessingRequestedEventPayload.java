package com.example.interhubdev.fileasset.internal.integration;

import java.util.UUID;

/**
 * Payload of the fileasset processing request integration event.
 */
public record FileAssetProcessingRequestedEventPayload(
    UUID fileAssetId
) {
}
