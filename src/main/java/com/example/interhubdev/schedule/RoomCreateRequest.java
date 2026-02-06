package com.example.interhubdev.schedule;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/**
 * Request body for creating a single room or one item in bulk room creation.
 * buildingId references an existing building.
 */
public record RoomCreateRequest(
    @NotNull(message = "Building id is required") UUID buildingId,
    @NotBlank(message = "Number is required") String number,
    @Min(value = 0, message = "Capacity must be >= 0") Integer capacity,
    String type
) {
}
