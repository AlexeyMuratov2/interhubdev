package com.example.interhubdev.outbox.internal;

/**
 * Status of an outbox event.
 */
enum OutboxEventStatus {
    /**
     * Event is new and ready for processing.
     */
    NEW,

    /**
     * Event is currently being processed by a worker.
     */
    PROCESSING,

    /**
     * Event was successfully processed.
     */
    DONE,

    /**
     * Event processing failed and will be retried.
     */
    FAILED
}
