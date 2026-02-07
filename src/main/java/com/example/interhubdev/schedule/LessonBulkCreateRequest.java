package com.example.interhubdev.schedule;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

/**
 * Request to create a single lesson as part of a bulk creation batch.
 * Lesson owns time (startTime, endTime). timeslotId optional (UI hint).
 * offeringSlotId optional — the offering slot this lesson was generated from (for lesson type and teacher on UI).
 */
public record LessonBulkCreateRequest(
    UUID offeringId,
    UUID offeringSlotId,
    LocalDate date,
    LocalTime startTime,
    LocalTime endTime,
    UUID timeslotId,
    UUID roomId,
    String status
) {
}
