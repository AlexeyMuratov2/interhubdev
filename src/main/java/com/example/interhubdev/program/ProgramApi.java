package com.example.interhubdev.program;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Public API for Program module: programs, curricula, curriculum subjects.
 */
public interface ProgramApi {

    // --- Program ---
    Optional<ProgramDto> findProgramById(UUID id);

    /**
     * Find programs by ids (batch). Missing ids are skipped; order is not guaranteed.
     *
     * @param ids program ids (must not be null)
     * @return list of program DTOs found (never null)
     */
    List<ProgramDto> findProgramsByIds(Collection<UUID> ids);

    Optional<ProgramDto> findProgramByCode(String code);

    List<ProgramDto> findAllPrograms();

    ProgramDto createProgram(String code, String name, String description, String degreeLevel, UUID departmentId);

    ProgramDto updateProgram(UUID id, String name, String description, String degreeLevel, UUID departmentId);

    void deleteProgram(UUID id);

    // --- Curriculum ---
    Optional<CurriculumDto> findCurriculumById(UUID id);

    /**
     * Find curricula by ids (batch). Missing ids are skipped; order is not guaranteed.
     *
     * @param ids curriculum ids (must not be null)
     * @return list of curriculum DTOs found (never null)
     */
    List<CurriculumDto> findCurriculaByIds(Collection<UUID> ids);

    List<CurriculumDto> findCurriculaByProgramId(UUID programId);

    CurriculumDto createCurriculum(UUID programId, String version, int durationYears, boolean isActive, String notes);

    CurriculumDto updateCurriculum(UUID id, String version, int durationYears, boolean isActive, CurriculumStatus status, String notes);

    CurriculumDto approveCurriculum(UUID id, UUID approvedBy);

    void deleteCurriculum(UUID id);

    /**
     * Resolve semester ID for a group's curriculum position (course year + semester number).
     * Calendar year is computed as group.startYear + (courseYear - 1); then the semester
     * with that year and number (1 or 2) is looked up in the academic calendar.
     * Course year is validated against the curriculum's duration.
     *
     * @param groupId    group ID (group contains curriculum and startYear)
     * @param courseYear course year (1-based)
     * @param semesterNo semester number within the year (1 or 2)
     * @return semester ID if group, curriculum and semester exist; otherwise throws NOT_FOUND
     * @throws com.example.interhubdev.error.AppException NOT_FOUND if group not found, curriculum not found, or semester not found for the computed year and number
     * @throws com.example.interhubdev.error.AppException BAD_REQUEST if courseYear or semesterNo are invalid
     */
    UUID getSemesterIdForCurriculumCourseAndSemester(UUID groupId, int courseYear, int semesterNo);

    // --- Curriculum subject ---
    Optional<CurriculumSubjectDto> findCurriculumSubjectById(UUID id);

    /**
     * Find curriculum subjects by IDs. Missing IDs are skipped.
     *
     * @param ids curriculum subject IDs (empty collection returns empty list)
     * @return list of curriculum subject DTOs for found IDs
     */
    List<CurriculumSubjectDto> findCurriculumSubjectsByIds(java.util.Collection<UUID> ids);

    /**
     * Get subject display names by curriculum subject ids (batch). Name is englishName or chineseName or code.
     *
     * @param curriculumSubjectIds curriculum subject ids (must not be null)
     * @return map curriculumSubjectId -> subject display name; missing ids are absent from map
     */
    Map<UUID, String> getSubjectNamesByCurriculumSubjectIds(List<UUID> curriculumSubjectIds);


    List<CurriculumSubjectDto> findCurriculumSubjectsByCurriculumId(UUID curriculumId);

    CurriculumSubjectDto createCurriculumSubject(
            UUID curriculumId,
            UUID subjectId,
            int semesterNo,
            Integer courseYear,
            int durationWeeks,
            Integer hoursTotal,
            Integer hoursLecture,
            Integer hoursPractice,
            Integer hoursLab,
            Integer hoursSeminar,
            Integer hoursSelfStudy,
            Integer hoursConsultation,
            Integer hoursCourseWork,
            UUID assessmentTypeId,
            java.math.BigDecimal credits
    );

    CurriculumSubjectDto updateCurriculumSubject(
            UUID id,
            Integer courseYear,
            Integer hoursTotal,
            Integer hoursLecture,
            Integer hoursPractice,
            Integer hoursLab,
            Integer hoursSeminar,
            Integer hoursSelfStudy,
            Integer hoursConsultation,
            Integer hoursCourseWork,
            UUID assessmentTypeId,
            java.math.BigDecimal credits
    );

    void deleteCurriculumSubject(UUID id);

    // --- Curriculum subject assessment ---
    List<CurriculumSubjectAssessmentDto> findAssessmentsByCurriculumSubjectId(UUID curriculumSubjectId);

    CurriculumSubjectAssessmentDto createCurriculumSubjectAssessment(
            UUID curriculumSubjectId,
            UUID assessmentTypeId,
            Integer weekNumber,
            boolean isFinal,
            java.math.BigDecimal weight,
            String notes
    );

    CurriculumSubjectAssessmentDto updateCurriculumSubjectAssessment(
            UUID id,
            UUID assessmentTypeId,
            Integer weekNumber,
            Boolean isFinal,
            java.math.BigDecimal weight,
            String notes
    );

    void deleteCurriculumSubjectAssessment(UUID id);

    // --- Curriculum practice ---
    List<CurriculumPracticeDto> findPracticesByCurriculumId(UUID curriculumId);

    CurriculumPracticeDto createCurriculumPractice(
            UUID curriculumId,
            PracticeType practiceType,
            String name,
            String description,
            int semesterNo,
            int durationWeeks,
            java.math.BigDecimal credits,
            UUID assessmentTypeId,
            PracticeLocation locationType,
            boolean supervisorRequired,
            boolean reportRequired,
            String notes
    );

    CurriculumPracticeDto updateCurriculumPractice(
            UUID id,
            PracticeType practiceType,
            String name,
            String description,
            Integer semesterNo,
            Integer durationWeeks,
            java.math.BigDecimal credits,
            UUID assessmentTypeId,
            PracticeLocation locationType,
            Boolean supervisorRequired,
            Boolean reportRequired,
            String notes
    );

    void deleteCurriculumPractice(UUID id);
}
