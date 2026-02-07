package com.example.interhubdev.schedule;

import java.util.UUID;

/**
 * Minimal room data for schedule display (id, number, building name).
 */
public record RoomSummaryDto(
    UUID id,
    String number,
    String buildingName
) {
}
