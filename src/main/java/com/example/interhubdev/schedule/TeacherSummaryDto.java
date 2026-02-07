package com.example.interhubdev.schedule;

import java.util.UUID;

/**
 * Minimal teacher data for schedule display (id, display name).
 */
public record TeacherSummaryDto(
    UUID id,
    String displayName
) {
}
