package com.example.interhubdev.subject.internal;

import com.example.interhubdev.subject.AssessmentTypeDto;
import com.example.interhubdev.subject.SubjectDto;

/**
 * Maps subject-module entities to their public DTOs.
 * Centralizes mapping logic so services and controllers stay free of conversion code.
 */
final class SubjectMappers {

    private SubjectMappers() {
    }

    /**
     * Maps a {@link Subject} entity to {@link SubjectDto}.
     *
     * @param e the subject entity (must not be null)
     * @return DTO with id, code, chineseName, englishName, description, departmentId, createdAt, updatedAt
     */
    static SubjectDto toSubjectDto(Subject e) {
        return new SubjectDto(
                e.getId(),
                e.getCode(),
                e.getChineseName(),
                e.getEnglishName(),
                e.getDescription(),
                e.getDepartmentId(),
                e.getCreatedAt(),
                e.getUpdatedAt()
        );
    }

    /**
     * Maps an {@link AssessmentType} entity to {@link AssessmentTypeDto}.
     *
     * @param e the assessment type entity (must not be null)
     * @return DTO with id, code, chineseName, englishName, isGraded, isFinal, sortOrder, createdAt
     */
    static AssessmentTypeDto toAssessmentTypeDto(AssessmentType e) {
        return new AssessmentTypeDto(
                e.getId(),
                e.getCode(),
                e.getChineseName(),
                e.getEnglishName(),
                e.getIsGraded(),
                e.getIsFinal(),
                e.getSortOrder(),
                e.getCreatedAt()
        );
    }
}
