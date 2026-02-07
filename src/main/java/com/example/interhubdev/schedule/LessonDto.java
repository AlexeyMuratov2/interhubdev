package com.example.interhubdev.schedule;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

/**
 * DTO for lesson. Lesson owns date and time (startTime, endTime).
 * timeslotId is optional (UI hint when created from a slot).
 * offeringSlotId references the offering slot this lesson was generated from (for lesson type and teacher on UI).
 */
public record LessonDto(
    UUID id,
    UUID offeringId,
    UUID offeringSlotId,
    LocalDate date,
    @JsonFormat(pattern = "HH:mm:ss") LocalTime startTime,
    @JsonFormat(pattern = "HH:mm:ss") LocalTime endTime,
    UUID timeslotId,
    UUID roomId,
    String topic,
    String status,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
}
