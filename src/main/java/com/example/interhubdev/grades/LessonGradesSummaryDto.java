package com.example.interhubdev.grades;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Summary of grade points assigned for a single lesson (lesson session).
 * Only ACTIVE entries with lesson_id = lessonSessionId are included; per-student totals.
 */
public record LessonGradesSummaryDto(
        UUID lessonSessionId,
        List<LessonGradeRowDto> rows
) {
    /**
     * Total points for one student for this lesson (sum of ACTIVE entries linked to this lesson).
     */
    public record LessonGradeRowDto(
            UUID studentId,
            BigDecimal totalPoints
    ) {
    }
}
