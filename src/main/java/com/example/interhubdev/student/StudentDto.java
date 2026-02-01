package com.example.interhubdev.student;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Data Transfer Object for Student profile.
 */
public record StudentDto(
    UUID id,
    UUID userId,
    String studentId,
    String chineseName,
    String faculty,
    String course,
    Integer enrollmentYear,
    String groupName,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
}
