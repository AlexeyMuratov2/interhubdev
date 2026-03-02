package com.example.interhubdev.composition;

import java.util.UUID;

/**
 * Read-only query API for lesson-related composition use cases.
 * Aggregates data from schedule, offering, subject, group, document, attendance, grades for lesson screens.
 */
public interface LessonQueryApi {

    /**
     * Get full details for a lesson (Lesson Full Details screen).
     * Aggregates subject, group, materials, homework, room, teachers, offering.
     *
     * @param lessonId    lesson ID (must not be null)
     * @param requesterId current authenticated user ID (for access control)
     * @return aggregated lesson full details DTO
     * @throws com.example.interhubdev.error.AppException NOT_FOUND if lesson not found, UNAUTHORIZED if requesterId is null
     */
    LessonFullDetailsDto getLessonFullDetails(UUID lessonId, UUID requesterId);

    /**
     * Get roster attendance for a lesson: students in group with attendance status and absence notices.
     *
     * @param lessonId        lesson (session) ID
     * @param requesterId     current authenticated user ID (teacher of lesson or admin)
     * @param includeCanceled if true, include CANCELED absence notices
     * @return roster with student display data, attendance status, notices per row
     * @throws com.example.interhubdev.error.AppException NOT_FOUND if lesson/offering/group not found, FORBIDDEN if requester cannot view
     */
    LessonRosterAttendanceDto getLessonRosterAttendance(UUID lessonId, UUID requesterId, boolean includeCanceled);

    /**
     * Get all homework submissions for a lesson: students in group with submission/points/files per homework.
     *
     * @param lessonId    lesson (session) ID
     * @param requesterId current authenticated user ID (must have permission to view submissions and grades)
     * @return aggregated DTO: lesson, group, homeworks, one row per student with items per homework
     * @throws com.example.interhubdev.error.AppException NOT_FOUND if lesson/offering/group not found, FORBIDDEN if requester cannot view
     */
    LessonHomeworkSubmissionsDto getLessonHomeworkSubmissions(UUID lessonId, UUID requesterId);
}
