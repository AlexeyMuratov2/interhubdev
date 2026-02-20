package com.example.interhubdev.grades;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/**
 * Request body for setting or replacing points for a student for a single lesson.
 * Used by PUT /api/grades/lessons/{lessonId}/students/{studentId}.
 */
public record SetLessonPointsRequest(
        @NotNull(message = "points is required")
        @DecimalMin(value = "-9999.99", message = "points must be at least -9999.99")
        @DecimalMax(value = "9999.99", message = "points must be at most 9999.99")
        BigDecimal points
) {
}
