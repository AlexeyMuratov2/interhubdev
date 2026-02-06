package com.example.interhubdev.schedule;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

/**
 * DTO for lesson. Lesson owns date and time (startTime, endTime).
 * timeslotId is optional (UI hint when created from a slot).
 */
public record LessonDto(
    UUID id,
    UUID offeringId,
    LocalDate date,
    LocalTime startTime,
    LocalTime endTime,
    UUID timeslotId,
    UUID roomId,
    String topic,
    String status,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
}
