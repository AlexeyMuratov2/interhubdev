package com.example.interhubdev.grades.internal;

import com.example.interhubdev.grades.GradeEntryDto;
import com.example.interhubdev.grades.GradeTypeCode;

import java.util.Optional;
import java.util.UUID;

/**
 * Entity to DTO mapping for grade entries. No instantiation.
 */
final class GradeEntryMappers {

    private GradeEntryMappers() {
    }

    static GradeEntryDto toDto(GradeEntryEntity e) {
        return new GradeEntryDto(
                e.getId(),
                e.getStudentId(),
                e.getOfferingId(),
                e.getPoints(),
                e.getTypeCode(),
                Optional.ofNullable(e.getTypeLabel()).filter(s -> !s.isBlank()),
                Optional.ofNullable(e.getDescription()).filter(s -> !s.isBlank()),
                Optional.ofNullable(e.getLessonId()),
                Optional.ofNullable(e.getHomeworkSubmissionId()),
                e.getGradedBy(),
                e.getGradedAt(),
                e.getStatus()
        );
    }
}
