package com.example.interhubdev.schedule;

import java.time.LocalTime;
import java.util.UUID;

public record TimeslotDto(
    UUID id,
    int dayOfWeek,
    LocalTime startTime,
    LocalTime endTime
) {
}
