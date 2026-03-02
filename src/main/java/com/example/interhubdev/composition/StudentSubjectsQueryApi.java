package com.example.interhubdev.composition;

import java.util.Optional;
import java.util.UUID;

/**
 * Read-only query API for student dashboard: subjects and subject detail.
 */
public interface StudentSubjectsQueryApi {

    /**
     * Get all subjects for which the current student has at least one lesson.
     * For student dashboard / subject list.
     *
     * @param requesterId current authenticated user ID (must be a student)
     * @param semesterNo   optional curriculum semester number (1, 2, 3, …); if empty, all semesters
     * @return DTO with list of subject DTOs
     * @throws com.example.interhubdev.error.AppException UNAUTHORIZED if requesterId is null, FORBIDDEN if not a student
     */
    StudentSubjectsDto getStudentSubjects(UUID requesterId, Optional<Integer> semesterNo);

    /**
     * Get full subject detail for a student by offering ID (Subject detail screen).
     * Subject, curriculum, offering schedule, teacher profiles, student stats, course materials with files.
     *
     * @param offeringId  offering ID (must not be null)
     * @param requesterId current authenticated user ID (must be a student in the group or admin)
     * @param semesterId  optional semester; if empty, current semester is used for statistics
     * @return aggregated DTO with subject, teachers, student stats, materials
     * @throws com.example.interhubdev.error.AppException NOT_FOUND if offering or related data not found, FORBIDDEN if student not in offering's group
     */
    StudentSubjectInfoDto getStudentSubjectInfo(UUID offeringId, UUID requesterId, Optional<UUID> semesterId);
}
