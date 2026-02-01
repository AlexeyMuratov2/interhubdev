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

    CurriculumDto createCurriculum(UUID programId, String version, int startYear, boolean isActive, String notes);

    CurriculumDto updateCurriculum(UUID id, String version, int startYear, boolean isActive, String notes);

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
            UUID assessmentTypeId,
            boolean isElective,
            java.math.BigDecimal credits
    );

    CurriculumSubjectDto updateCurriculumSubject(
            UUID id,
            Integer courseYear,
            Integer hoursTotal,
            Integer hoursLecture,
            Integer hoursPractice,
            Integer hoursLab,
            UUID assessmentTypeId,
            Boolean isElective,
            java.math.BigDecimal credits
    );

    void deleteCurriculumSubject(UUID id);
}
