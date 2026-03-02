package com.example.interhubdev.composition;

import java.util.Optional;
import java.util.UUID;

/**
 * Read-only query API for teacher: group + subject info (students, points, submissions, attendance).
 */
public interface GroupSubjectQueryApi {

    /**
     * Get full info for a group and subject (teacher's "Group subject info" screen).
     * Only teachers assigned to an offering slot for this subject and group (or admin) can view.
     *
     * @param groupId     group ID (must not be null)
     * @param subjectId   subject ID (must not be null)
     * @param requesterId current authenticated user ID (must be teacher of this offering or admin)
     * @param semesterId  optional semester; if empty, current semester is used
     * @return aggregated DTO: subject, offering, slots, curriculum, per-student stats (points, submissions, attendance percent)
     * @throws com.example.interhubdev.error.AppException NOT_FOUND if group or offering not found, FORBIDDEN if requester is not teacher of this offering
     */
    GroupSubjectInfoDto getGroupSubjectInfo(UUID groupId, UUID subjectId, UUID requesterId, Optional<UUID> semesterId);
}
