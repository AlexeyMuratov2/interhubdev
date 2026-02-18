package com.example.interhubdev.subject;

import java.util.List;
import java.util.UUID;

/**
 * Port for offering lookup by teacher ID.
 * Implemented by adapter to avoid direct dependency on offering module.
 */
public interface OfferingLookupPort {

    /**
     * Get all offerings where teacher is assigned (as main teacher, slot teacher, or offering teacher).
     *
     * @param teacherId teacher entity ID
     * @return list of offering DTOs; empty if teacher has no offerings
     */
    List<GroupSubjectOfferingDto> findOfferingsByTeacherId(UUID teacherId);

    /**
     * Get offerings for a specific curriculum subject where teacher is assigned.
     *
     * @param curriculumSubjectId curriculum subject ID
     * @param teacherId teacher entity ID
     * @return list of offering DTOs; empty if teacher has no offerings for this curriculum subject
     */
    List<GroupSubjectOfferingDto> findOfferingsByCurriculumSubjectIdAndTeacherId(
            UUID curriculumSubjectId, UUID teacherId);
}
