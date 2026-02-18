package com.example.interhubdev.grades;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

/**
 * One student entry in a bulk create request (POST /api/grades/entries/bulk).
 */
public record BulkGradeItem(
    @NotNull(message = "studentId is required")
    UUID studentId,
    @NotNull(message = "points is required")
    @DecimalMin(value = "-9999.99", message = "points must be at least -9999.99")
    @DecimalMax(value = "9999.99", message = "points must be at most 9999.99")
    BigDecimal points,
    UUID homeworkSubmissionId
) {
    public Optional<UUID> homeworkSubmissionIdOptional() {
        return Optional.ofNullable(homeworkSubmissionId);
    }
}
