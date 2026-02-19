package com.example.interhubdev.offering;

import java.util.UUID;

/**
 * Teacher assigned to an offering (derived from main teacher and slot teachers).
 * Role is null or "MAIN" for the offering's main teacher; LECTURE, PRACTICE, LAB for slot teachers.
 */
public record OfferingTeacherItemDto(
    UUID teacherId,
    String role
) {
}
