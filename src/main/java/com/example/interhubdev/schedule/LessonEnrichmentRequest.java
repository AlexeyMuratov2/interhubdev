package com.example.interhubdev.schedule;

import java.util.UUID;

/**
 * Request item for LessonEnrichmentPort: offering and slot ids for one lesson.
 *
 * @param offeringId offering the lesson belongs to
 * @param slotId     offering slot id, or null for legacy lessons
 */
public record LessonEnrichmentRequest(UUID offeringId, UUID slotId) {
}
