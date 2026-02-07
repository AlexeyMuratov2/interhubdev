package com.example.interhubdev.schedule;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Port for teacher lookup by ids (batch). Implemented by adapter so that
 * Schedule does not depend on Teacher module.
 */
public interface TeacherLookupPort {

    /**
     * Get teacher summaries by ids (batch). Missing ids are absent from the map.
     *
     * @param teacherIds teacher entity ids (must not be null)
     * @return map teacherId -> TeacherSummaryDto
     */
    Map<UUID, TeacherSummaryDto> getTeacherSummaries(List<UUID> teacherIds);
}
