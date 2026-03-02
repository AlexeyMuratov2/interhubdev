package com.example.interhubdev.submission.internal;

/**
 * Event type constants for submission integration events (outbox).
 */
public final class SubmissionEventTypes {

    private SubmissionEventTypes() {
    }

    /** Fired when a student submits (or replaces) a homework solution. */
    public static final String HOMEWORK_SUBMISSION_SUBMITTED = "submission.homework_submission.submitted";
}
