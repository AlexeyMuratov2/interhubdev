package com.example.interhubdev.offering;

import com.example.interhubdev.program.CurriculumSubjectDto;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Port for looking up curriculum subject information from the Program module.
 * Used to validate curriculum subject existence and to get subject names.
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
     * Get subject display names by curriculum subject ids (batch). Name is englishName or chineseName or code.
     *
     * @param curriculumSubjectIds curriculum subject ids (must not be null)
     * @return map curriculumSubjectId -> subject display name; missing ids are absent from map
     */
    Map<UUID, String> getSubjectNamesByCurriculumSubjectIds(List<UUID> curriculumSubjectIds);
}
