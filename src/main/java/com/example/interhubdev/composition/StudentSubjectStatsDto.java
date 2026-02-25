package com.example.interhubdev.composition;

import java.math.BigDecimal;

/**
 * Student-specific statistics for a subject (offering) within the current semester.
 * Attendance uses the same rules as group subject info: LATE is not absence; percentage = (PRESENT + LATE) / totalMarked * 100.
 */
public record StudentSubjectStatsDto(
    /** Attendance percentage; null if no lessons with attendance marks exist. */
    Double attendancePercent,
    /** Number of distinct homework assignments the student has submitted. */
    int submittedHomeworkCount,
    /** Total homework assignments available for this offering in the semester. */
    int totalHomeworkCount,
    /** Sum of all ACTIVE grade entries for the student in this offering. */
    BigDecimal totalPoints
) {
}
