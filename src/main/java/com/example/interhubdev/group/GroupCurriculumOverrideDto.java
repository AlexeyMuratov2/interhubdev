package com.example.interhubdev.group;

import java.time.LocalDateTime;
import java.util.UUID;

public record GroupCurriculumOverrideDto(
    UUID id,
    UUID groupId,
    UUID curriculumSubjectId,
    UUID subjectId,
    String action,
    UUID newAssessmentTypeId,
    Integer newDurationWeeks,
    String reason,
    LocalDateTime createdAt
) {
}
