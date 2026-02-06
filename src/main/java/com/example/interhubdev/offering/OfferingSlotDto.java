package com.example.interhubdev.offering;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Public DTO for an offering weekly slot: defines when a lesson type occurs.
 */
public record OfferingSlotDto(
    UUID id,
    UUID offeringId,
    UUID timeslotId,
    String lessonType,
    UUID roomId,
    UUID teacherId,
    LocalDateTime createdAt
) {
}
