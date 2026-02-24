package com.example.interhubdev.composition;

import com.example.interhubdev.grades.GradeEntryDto;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Full grade history for a student in an offering: all grade entries with lesson, homework,
 * submission and grader context. For frontend to display how, when and for what each grade was given.
 */
public record StudentGradeHistoryDto(
    UUID studentId,
    UUID offeringId,
    /** Sum of ACTIVE entries only. */
    BigDecimal totalPoints,
    /** Sum of points per type code (e.g. SEMINAR, HOMEWORK). Only ACTIVE entries. */
    Map<String, BigDecimal> breakdownByType,
    /** Entries ordered by gradedAt descending; each item includes full context (lesson, homework, submission, gradedBy). */
    List<StudentGradeHistoryItemDto> entries
) {
}
