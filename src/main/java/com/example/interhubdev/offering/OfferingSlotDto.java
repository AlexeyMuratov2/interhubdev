package com.example.interhubdev.offering;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

/**
 * DTO for offering weekly slot. Slot owns day and time; timeslotId is optional (UI hint).
 */
public record OfferingSlotDto(
    UUID id,
    UUID offeringId,
    int dayOfWeek,
    LocalTime startTime,
    LocalTime endTime,
    UUID timeslotId,
    String lessonType,
    UUID roomId,
    UUID teacherId,
    LocalDateTime createdAt
) {
}
