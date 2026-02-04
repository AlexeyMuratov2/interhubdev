package com.example.interhubdev.subject;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Public API for Subject module: subjects and assessment types.
 */
public interface SubjectApi {

    // --- Subject ---
    Optional<SubjectDto> findSubjectById(UUID id);

    Optional<SubjectDto> findSubjectByCode(String code);

    List<SubjectDto> findAllSubjects();

    SubjectDto createSubject(String code, String name, String description, UUID departmentId);

    SubjectDto updateSubject(UUID id, String name, String description, UUID departmentId);

    void deleteSubject(UUID id);

    // --- Assessment type ---
    Optional<AssessmentTypeDto> findAssessmentTypeById(UUID id);

    Optional<AssessmentTypeDto> findAssessmentTypeByCode(String code);

    List<AssessmentTypeDto> findAllAssessmentTypes();

    AssessmentTypeDto createAssessmentType(String code, String name, Boolean isGraded, Boolean isFinal, Integer sortOrder);

    AssessmentTypeDto updateAssessmentType(UUID id, String name, Boolean isGraded, Boolean isFinal, Integer sortOrder);

    void deleteAssessmentType(UUID id);
}
