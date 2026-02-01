package com.example.interhubdev.schedule;

import java.time.LocalDateTime;
import java.util.UUID;

public record RoomDto(
    UUID id,
    String building,
    String number,
    Integer capacity,
    String type,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
}
