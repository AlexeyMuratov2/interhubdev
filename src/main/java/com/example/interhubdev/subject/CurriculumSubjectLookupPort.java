package com.example.interhubdev.subject;

import com.example.interhubdev.program.CurriculumSubjectAssessmentDto;
import com.example.interhubdev.program.CurriculumSubjectDto;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Port for looking up curriculum subject information from the Program module.
 * Used by teacher subject endpoints to access curriculum subject data.
 * <p>
 * Implemented by an adapter in the adapter package using ProgramApi.
 */
public interface CurriculumSubjectLookupPort {

    /**
     * Find curriculum subject by ID.
     *
     * @param id curriculum subject ID
     * @return optional curriculum subject DTO if found
     */
    Optional<CurriculumSubjectDto> findById(UUID id);

    /**
     * Find all assessments for a curriculum subject.
     *
     * @param curriculumSubjectId curriculum subject ID
     * @return list of assessment DTOs
     */
    List<CurriculumSubjectAssessmentDto> findAssessmentsByCurriculumSubjectId(UUID curriculumSubjectId);
}
