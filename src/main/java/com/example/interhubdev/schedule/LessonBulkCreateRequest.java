package com.example.interhubdev.schedule;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

/**
 * Request to create a single lesson as part of a bulk creation batch.
 * Lesson owns time (startTime, endTime). timeslotId optional (UI hint).
 */
public record LessonBulkCreateRequest(
    UUID offeringId,
    LocalDate date,
    LocalTime startTime,
    LocalTime endTime,
    UUID timeslotId,
    UUID roomId,
    String status
) {
}
