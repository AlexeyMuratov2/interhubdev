package com.example.interhubdev.schedule;

import java.util.UUID;

/**
 * Minimal group data for schedule display (id, code, name).
 */
public record GroupSummaryDto(
    UUID id,
    String code,
    String name
) {
}
