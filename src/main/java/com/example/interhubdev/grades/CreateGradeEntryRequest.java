package com.example.interhubdev.grades;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * Request body for POST /api/grades/entries (single entry).
 */
public record CreateGradeEntryRequest(
    @NotNull(message = "studentId is required")
    UUID studentId,
    @NotNull(message = "offeringId is required")
    UUID offeringId,
    @NotNull(message = "points is required")
    @DecimalMin(value = "-9999.99", message = "points must be at least -9999.99")
    @DecimalMax(value = "9999.99", message = "points must be at most 9999.99")
    BigDecimal points,
    @NotNull(message = "typeCode is required")
    GradeTypeCode typeCode,
    @Size(max = 255)
    String typeLabel,
    @Size(max = 2000)
    String description,
    UUID lessonSessionId,
    UUID homeworkSubmissionId,
    LocalDateTime gradedAt
) {
    public Optional<String> typeLabelOptional() {
        return Optional.ofNullable(typeLabel).filter(s -> !s.isBlank());
    }
    public Optional<String> descriptionOptional() {
        return Optional.ofNullable(description).filter(s -> !s.isBlank());
    }
    public Optional<UUID> lessonSessionIdOptional() {
        return Optional.ofNullable(lessonSessionId);
    }
    public Optional<UUID> homeworkSubmissionIdOptional() {
        return Optional.ofNullable(homeworkSubmissionId);
    }
    public Optional<LocalDateTime> gradedAtOptional() {
        return Optional.ofNullable(gradedAt);
    }
}
