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

    SubjectDto createSubject(String code, String name, String description);

    SubjectDto updateSubject(UUID id, String name, String description);

    void deleteSubject(UUID id);

    // --- Assessment type ---
    Optional<AssessmentTypeDto> findAssessmentTypeById(UUID id);

    Optional<AssessmentTypeDto> findAssessmentTypeByCode(String code);

    List<AssessmentTypeDto> findAllAssessmentTypes();

    AssessmentTypeDto createAssessmentType(String code, String name);

    void deleteAssessmentType(UUID id);
}
