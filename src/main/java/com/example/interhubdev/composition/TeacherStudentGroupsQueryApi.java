package com.example.interhubdev.composition;

import java.util.UUID;

/**
 * Read-only query API for teacher dashboard: student groups where the teacher has at least one lesson.
 */
public interface TeacherStudentGroupsQueryApi {

    /**
     * Get student groups where the current teacher has at least one lesson (slots with lessons only).
     * For teacher dashboard "Student groups" page.
     *
     * @param requesterId current authenticated user ID (must be a teacher)
     * @return aggregated DTO with list of groups and enriched data (program, curriculum, curator, student count)
     * @throws com.example.interhubdev.error.AppException UNAUTHORIZED if requesterId is null, FORBIDDEN if not a teacher
     */
    TeacherStudentGroupsDto getTeacherStudentGroups(UUID requesterId);
}
