package com.example.interhubdev.program;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record CurriculumPracticeDto(
    UUID id,
    UUID curriculumId,
    PracticeType practiceType,
    String name,
    String description,
    int semesterNo,
    int durationWeeks,
    BigDecimal credits,
    UUID assessmentTypeId,
    PracticeLocation locationType,
    boolean supervisorRequired,
    boolean reportRequired,
    String notes,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
}
