package com.example.interhubdev.program;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Public API for Program module: programs, curricula, curriculum subjects.
 */
public interface ProgramApi {

    // --- Program ---
    Optional<ProgramDto> findProgramById(UUID id);

    Optional<ProgramDto> findProgramByCode(String code);

    List<ProgramDto> findAllPrograms();

    ProgramDto createProgram(String code, String name, String description, String degreeLevel, UUID departmentId);

    ProgramDto updateProgram(UUID id, String name, String description, String degreeLevel, UUID departmentId);

    void deleteProgram(UUID id);

    // --- Curriculum ---
    Optional<CurriculumDto> findCurriculumById(UUID id);

    List<CurriculumDto> findCurriculaByProgramId(UUID programId);

    CurriculumDto createCurriculum(UUID programId, String version, int startYear, Integer endYear, boolean isActive, String notes);

    CurriculumDto updateCurriculum(UUID id, String version, int startYear, Integer endYear, boolean isActive, CurriculumStatus status, String notes);

    CurriculumDto approveCurriculum(UUID id, UUID approvedBy);

    void deleteCurriculum(UUID id);

    // --- Curriculum subject ---
    Optional<CurriculumSubjectDto> findCurriculumSubjectById(UUID id);

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
