package com.example.interhubdev.schedule;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO for room. buildingId and buildingName refer to the building (campus block) the room belongs to.
 */
public record RoomDto(
    UUID id,
    UUID buildingId,
    String buildingName,
    String number,
    Integer capacity,
    String type,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
}
