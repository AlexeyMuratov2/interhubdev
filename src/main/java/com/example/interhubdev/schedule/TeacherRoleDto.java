package com.example.interhubdev.schedule;

import java.util.UUID;

/**
 * Teacher attached to an offering with role (LECTURE, PRACTICE, LAB).
 */
public record TeacherRoleDto(
    UUID teacherId,
    String role
) {
}
