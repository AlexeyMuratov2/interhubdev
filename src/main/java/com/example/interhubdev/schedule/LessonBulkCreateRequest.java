package com.example.interhubdev.schedule;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Request to create a single lesson as part of a bulk creation batch.
 *
 * @param offeringId offering ID (required)
 * @param date lesson date (required)
 * @param timeslotId timeslot ID (required)
 * @param roomId room ID (nullable)
 * @param status lesson status, e.g. "planned" (nullable, defaults to "planned")
 */
public record LessonBulkCreateRequest(
    UUID offeringId,
    LocalDate date,
    UUID timeslotId,
    UUID roomId,
    String status
) {
}
