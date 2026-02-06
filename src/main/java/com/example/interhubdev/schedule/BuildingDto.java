package com.example.interhubdev.schedule;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO for building (campus block). Used in building CRUD and in room responses.
 */
public record BuildingDto(
    UUID id,
    String name,
    String address,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
}
