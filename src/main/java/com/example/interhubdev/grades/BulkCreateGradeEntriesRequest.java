package com.example.interhubdev.grades;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Request body for POST /api/grades/entries/bulk.
 * Common fields apply to all items; each item can override homeworkSubmissionId and points.
 */
public record BulkCreateGradeEntriesRequest(
    @NotNull(message = "offeringId is required")
    UUID offeringId,
    @NotNull(message = "typeCode is required")
    GradeTypeCode typeCode,
    @Size(max = 255)
    String typeLabel,
    @Size(max = 2000)
    String description,
    UUID lessonSessionId,
    LocalDateTime gradedAt,
    @NotEmpty(message = "items must not be empty")
    @Valid
    List<BulkGradeItem> items
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
    public Optional<LocalDateTime> gradedAtOptional() {
        return Optional.ofNullable(gradedAt);
    }
}
