package com.example.interhubdev.subject;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Public API for Subject module: subject catalog and assessment type catalog.
 * Used by REST controller and other modules (e.g. program for curriculum subjects).
 */
public interface SubjectApi {

    // --- Subject ---

    /**
     * Finds a subject by its unique id.
     *
     * @param id subject id (must not be null)
     * @return optional containing the subject DTO if found, empty otherwise
     */
    Optional<SubjectDto> findSubjectById(UUID id);

    /**
     * Finds a subject by its unique code (case-sensitive after trim).
     *
     * @param code subject code (may be null; empty optional returned if null/blank)
     * @return optional containing the subject DTO if found, empty otherwise
     */
    Optional<SubjectDto> findSubjectByCode(String code);

    /**
     * Returns all subjects in stable order (by code ascending).
     *
     * @return list of subject DTOs (never null, may be empty)
     */
    List<SubjectDto> findAllSubjects();

    /**
     * Find subjects by ids (batch). Missing ids are skipped; order is not guaranteed.
     *
     * @param ids subject ids (must not be null)
     * @return list of subject DTOs found (never null, may be empty)
     */
    List<SubjectDto> findSubjectsByIds(List<UUID> ids);

    /**
     * Creates a new subject. Code and chineseName are required; departmentId is optional but validated if set.
     *
     * @param code        required, trimmed
     * @param chineseName required; trimmed
     * @param englishName optional; trimmed, may be null
     * @param description optional; trimmed, may be null
     * @param departmentId optional; if non-null, department must exist
     * @return created subject DTO
     * @throws com.example.interhubdev.error.AppException BAD_REQUEST if code is blank, CONFLICT if code exists, NOT_FOUND if department not found
     */
    SubjectDto createSubject(String code, String chineseName, String englishName, String description, UUID departmentId);

    /**
     * Updates an existing subject by id. Only non-null fields are updated; departmentId is validated if set.
     *
     * @param id          subject id (must not be null)
     * @param chineseName optional; if non-null, trimmed and set
     * @param englishName optional; if non-null, trimmed and set
     * @param description optional; if non-null, trimmed and set
     * @param departmentId optional; if non-null, department must exist
     * @return updated subject DTO
     * @throws com.example.interhubdev.error.AppException NOT_FOUND if subject or department not found
     */
    SubjectDto updateSubject(UUID id, String chineseName, String englishName, String description, UUID departmentId);

    /**
     * Deletes a subject by id.
     *
     * @param id subject id (must not be null)
     * @throws com.example.interhubdev.error.AppException NOT_FOUND if subject does not exist
     */
    void deleteSubject(UUID id);

    // --- Assessment type ---

    /**
     * Finds an assessment type by its unique id.
     *
     * @param id assessment type id (must not be null)
     * @return optional containing the assessment type DTO if found, empty otherwise
     */
    Optional<AssessmentTypeDto> findAssessmentTypeById(UUID id);

    /**
     * Finds an assessment type by its unique code (case-sensitive after trim).
     *
     * @param code assessment type code (may be null; empty optional returned if null/blank)
     * @return optional containing the assessment type DTO if found, empty otherwise
     */
    Optional<AssessmentTypeDto> findAssessmentTypeByCode(String code);

    /**
     * Returns all assessment types in stable order (sort order ascending, then code ascending).
     *
     * @return list of assessment type DTOs (never null, may be empty)
     */
    List<AssessmentTypeDto> findAllAssessmentTypes();

    /**
     * Creates a new assessment type. Code and chineseName are required; optional fields have defaults.
     *
     * @param code        required, trimmed
     * @param chineseName required; trimmed
     * @param englishName optional; trimmed, may be null
     * @param isGraded    optional; default true
     * @param isFinal     optional; default false
     * @param sortOrder   optional; default 0
     * @return created assessment type DTO
     * @throws com.example.interhubdev.error.AppException BAD_REQUEST if code is blank, CONFLICT if code exists
     */
    AssessmentTypeDto createAssessmentType(String code, String chineseName, String englishName, Boolean isGraded, Boolean isFinal, Integer sortOrder);

    /**
     * Updates an existing assessment type by id. Only non-null fields are updated.
     *
     * @param id          assessment type id (must not be null)
     * @param chineseName optional; if non-null, trimmed and set
     * @param englishName optional; if non-null, trimmed and set
     * @param isGraded    optional; if non-null, set
     * @param isFinal     optional; if non-null, set
     * @param sortOrder   optional; if non-null, set
     * @return updated assessment type DTO
     * @throws com.example.interhubdev.error.AppException NOT_FOUND if assessment type does not exist
     */
    AssessmentTypeDto updateAssessmentType(UUID id, String chineseName, String englishName, Boolean isGraded, Boolean isFinal, Integer sortOrder);

    /**
     * Deletes an assessment type by id.
     *
     * @param id assessment type id (must not be null)
     * @throws com.example.interhubdev.error.AppException NOT_FOUND if assessment type does not exist
     */
    void deleteAssessmentType(UUID id);

    // --- Teacher subjects ---

    /**
     * Get list of teacher subjects (shortened view) filtered by semester.
     * Returns all subjects where teacher is assigned (as main teacher, slot teacher, or offering teacher).
     *
     * @param teacherId teacher entity ID (must not be null)
     * @param semesterNo optional semester number filter (1..N); if null, returns all semesters
     * @return list of teacher subject items (never null, may be empty)
     * @throws com.example.interhubdev.error.AppException if teacher not found
     */
    List<TeacherSubjectListItemDto> findTeacherSubjects(UUID teacherId, Integer semesterNo);

    /**
     * Get full detail of a teacher subject.
     * Returns subject data, curriculum subject data, assessments, and all offerings with materials.
     *
     * @param curriculumSubjectId curriculum subject ID (must not be null)
     * @param teacherId teacher entity ID (must not be null)
     * @param requesterId current authenticated user ID (for access control)
     * @return teacher subject detail DTO
     * @throws com.example.interhubdev.error.AppException NOT_FOUND if curriculum subject not found,
     *         FORBIDDEN if teacher does not have access to this curriculum subject
     */
    TeacherSubjectDetailDto findTeacherSubjectDetail(UUID curriculumSubjectId, UUID teacherId, UUID requesterId);
}
