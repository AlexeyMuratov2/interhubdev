package com.example.interhubdev.program;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record CurriculumSubjectAssessmentDto(
    UUID id,
    UUID curriculumSubjectId,
    UUID assessmentTypeId,
    Integer weekNumber,
    boolean isFinal,
    BigDecimal weight,
    String notes,
    LocalDateTime createdAt
) {
}
