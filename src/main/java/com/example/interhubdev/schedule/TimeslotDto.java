package com.example.interhubdev.schedule;

import java.time.LocalTime;
import java.util.UUID;

/**
 * DTO for timeslot — time template (day of week + time range) for UI when setting lesson time.
 */
public record TimeslotDto(
    UUID id,
    int dayOfWeek,
    LocalTime startTime,
    LocalTime endTime
) {
}
