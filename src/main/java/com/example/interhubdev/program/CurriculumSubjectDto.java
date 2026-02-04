package com.example.interhubdev.program;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record CurriculumSubjectDto(
    UUID id,
    UUID curriculumId,
    UUID subjectId,
    int semesterNo,
    Integer courseYear,
    int durationWeeks,
    Integer hoursTotal,
    Integer hoursLecture,
    Integer hoursPractice,
    Integer hoursLab,
    Integer hoursSeminar,
    Integer hoursSelfStudy,
    Integer hoursConsultation,
    Integer hoursCourseWork,
    UUID assessmentTypeId,
    BigDecimal credits,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
}
