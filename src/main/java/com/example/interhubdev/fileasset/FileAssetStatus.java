package com.example.interhubdev.fileasset;

/**
 * Canonical lifecycle states of a file asset.
 */
public enum FileAssetStatus {

    /**
     * File is registered and the policy is fixed, but bytes have not been confirmed yet.
     */
    REGISTERED,

    /**
     * Bytes are confirmed in temporary storage and the asset can enter background processing.
     */
    UPLOADED,

    /**
     * Background processing is in progress. Retries are only allowed in this state.
     */
    PROCESSING,

    /**
     * Processing completed successfully and the asset is ready for controlled delivery.
     */
    ACTIVE,

    /**
     * Processing failed permanently or exhausted retry budget. Terminal.
     */
    FAILED,

    /**
     * Asset was deleted intentionally. Terminal.
     */
    DELETED,

    /**
     * Asset expired before it was completed or claimed. Terminal.
     */
    EXPIRED;

    public boolean isTerminal() {
        return this == FAILED || this == DELETED || this == EXPIRED;
    }
}
