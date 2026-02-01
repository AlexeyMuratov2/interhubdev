package com.example.interhubdev.teacher;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Data Transfer Object for Teacher profile.
 */
public record TeacherDto(
    UUID id,
    UUID userId,
    String teacherId,
    String faculty,
    String englishName,
    String position,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
}
