package com.example.interhubdev.schedule;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

/**
 * Minimal offering slot data for schedule display (day, time, type, room, teacher).
 */
public record SlotSummaryDto(
    UUID id,
    UUID offeringId,
    int dayOfWeek,
    @JsonFormat(pattern = "HH:mm:ss") LocalTime startTime,
    @JsonFormat(pattern = "HH:mm:ss") LocalTime endTime,
    UUID timeslotId,
    String lessonType,
    UUID roomId,
    UUID teacherId,
    LocalDateTime createdAt
) {
}
