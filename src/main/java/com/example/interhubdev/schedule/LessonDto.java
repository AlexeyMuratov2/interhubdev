package com.example.interhubdev.schedule;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record LessonDto(
    UUID id,
    UUID offeringId,
    LocalDate date,
    UUID timeslotId,
    UUID roomId,
    String topic,
    String status,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
}
