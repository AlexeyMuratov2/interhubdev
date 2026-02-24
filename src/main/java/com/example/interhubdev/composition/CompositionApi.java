package com.example.interhubdev.composition;

import java.util.UUID;

/**
 * Public API for Composition module: read-only data aggregation for complex UI screens.
 * This module aggregates data from multiple modules to reduce frontend request count.
 */
public interface CompositionApi {

    /**
     * Get full details for a lesson (Use Case #1: Lesson Full Details).
     * Aggregates all data needed for the "Full Lesson Information" screen:
     * - Subject information (name and all available subject data)
     * - Group information (groupId and basic group info if available)
     * - Lesson materials (all materials linked to the lesson)
     * - Homework assignments (all homework linked to the lesson)
     * - Lesson instance details (building, room, teacher, date/time, offering info, offering slot if present)
     *
     * @param lessonId lesson ID (must not be null)
     * @param requesterId current authenticated user ID (for access control)
     * @return aggregated lesson full details DTO
     * @throws com.example.interhubdev.error.AppException NOT_FOUND if lesson not found,
     *         UNAUTHORIZED if requesterId is null or invalid
     */
    LessonFullDetailsDto getLessonFullDetails(UUID lessonId, UUID requesterId);

    /**
     * Get roster attendance for a lesson (Use Case #2: Lesson attendance table).
     * Aggregates all students in the lesson's group with their attendance status and absence notices
     * for this lesson. Designed for the lesson screen attendance table UI.
     *
     * @param lessonId        lesson (session) ID (must not be null)
     * @param requesterId     current authenticated user ID (must be teacher of this lesson or admin)
     * @param includeCanceled if true, include CANCELED absence notices in each student's notices list
     * @return aggregated roster with student display data, attendance status, and notices per row
     * @throws com.example.interhubdev.error.AppException NOT_FOUND if lesson/offering/group not found,
     *         UNAUTHORIZED if requesterId is null, FORBIDDEN if requester cannot view session attendance
     */
    LessonRosterAttendanceDto getLessonRosterAttendance(UUID lessonId, UUID requesterId, boolean includeCanceled);

    /**
     * Get all homework submissions for a lesson (Use Case #3: Lesson homework submissions table).
     * Returns all students in the lesson's group; for each student and each homework of the lesson
     * either the submission with points and files, or empty (null submission, null points, empty files).
     *
     * @param lessonId    lesson (session) ID (must not be null)
     * @param requesterId current authenticated user ID (must have permission to view submissions and grades)
     * @return aggregated DTO: lesson, group, homeworks list, and one row per student with items per homework
     * @throws com.example.interhubdev.error.AppException NOT_FOUND if lesson/offering/group not found,
     *         UNAUTHORIZED if requesterId is null, FORBIDDEN if requester cannot view
     */
    LessonHomeworkSubmissionsDto getLessonHomeworkSubmissions(UUID lessonId, UUID requesterId);

    /**
     * Get student groups where the current teacher has at least one lesson (slots with lessons only).
     * For teacher dashboard "Student groups" page. Returns group, program, curriculum, curator user, and student count.
     *
     * @param requesterId current authenticated user ID (must be a teacher)
     * @return aggregated DTO with list of groups and enriched data
     * @throws com.example.interhubdev.error.AppException UNAUTHORIZED if requesterId is null,
     *         FORBIDDEN if requester is not a teacher
     */
    TeacherStudentGroupsDto getTeacherStudentGroups(UUID requesterId);
}
