package com.example.interhubdev.storedfile;

/**
 * Lifecycle status of a stored file. Only ACTIVE files are available for bind and download
 * (activation gate). DELETED is terminal and unrecoverable for delivery.
 */
public enum FileStatus {

    /** File received into temp area, not yet admitted for use. */
    RECEIVED_TEMP,

    /** Mandatory checks (e.g. AV) in progress. */
    PENDING_SECURITY_CHECKS,

    /** Checks passed, safety class assigned, file allowed for bind/download within its class. */
    ACTIVE,

    /** Rejected by policy or checks. */
    REJECTED,

    /** Deleted; terminal state — not available for delivery or bind. */
    DELETED
}
