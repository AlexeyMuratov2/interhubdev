package com.example.interhubdev.grades;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Summary of grade points assigned for a single lesson (lesson session).
 * Only ACTIVE entries with lesson_id = lessonSessionId and no homework submission (lesson points only).
 */
public record LessonGradesSummaryDto(
        UUID lessonSessionId,
        List<LessonGradeRowDto> rows
) {
    /**
     * Total lesson-only points for one student for this lesson (excludes homework points).
     */
    public record LessonGradeRowDto(
            UUID studentId,
            BigDecimal totalPoints
    ) {
    }
}
