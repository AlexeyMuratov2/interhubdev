package com.example.interhubdev.submission.internal.integration;

import java.time.Instant;
import java.util.UUID;

/**
 * Payload for homework submission submitted event.
 * Contains only IDs and timestamp; notification content (lesson title, subject name, student display name)
 * is resolved in the adapter via NotificationContentResolver.
 */
public record HomeworkSubmissionSubmittedEventPayload(
        UUID submissionId,
        UUID homeworkId,
        UUID lessonId,
        UUID authorId,
        Instant submittedAt
) {
}
