package com.example.interhubdev.composition;

import java.util.Optional;
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

    /**
     * Get full info for a group and subject (Use Case: Group subject info).
     * For the teacher's "Group subject info" screen. Data returned only if the requester is a teacher
     * assigned to an offering slot for this subject and group. Includes subject, offering, slots, curriculum,
     * semester, total homework count, and per-student stats (points, submitted homework count, attendance percent).
     * Attendance percent is from attendance module (only lessons with at least one mark count).
     *
     * @param groupId     group ID (must not be null)
     * @param subjectId   subject ID (must not be null)
     * @param requesterId current authenticated user ID (must be teacher of this offering or admin)
     * @param semesterId  optional semester; if empty, current semester is used
     * @return aggregated DTO
     * @throws com.example.interhubdev.error.AppException NOT_FOUND if group or offering for this group+subject not found,
     *         UNAUTHORIZED if requesterId is null, FORBIDDEN if requester is not a teacher of this offering
     */
    GroupSubjectInfoDto getGroupSubjectInfo(UUID groupId, UUID subjectId, UUID requesterId, Optional<UUID> semesterId);

    /**
     * Get full grade history for a student in an offering (Use Case: Student grade history).
     * Returns all grade entries (including voided) with full context: lesson (when grade is for a lesson),
     * homework and submission (when grade is for homework), and who graded. Enables frontend to show
     * how, when and for what each grade was given. Batch-loaded; no N+1.
     *
     * @param studentId   student profile ID (must not be null)
     * @param offeringId  offering ID (must not be null)
     * @param requesterId current authenticated user ID (must have permission to view grades)
     * @return aggregated DTO with totalPoints, breakdownByType, and entries with lesson/homework/submission/gradedBy
     * @throws com.example.interhubdev.error.AppException NOT_FOUND if offering not found,
     *         UNAUTHORIZED if requesterId is null, FORBIDDEN if requester cannot view grades
     */
    StudentGradeHistoryDto getStudentGradeHistory(UUID studentId, UUID offeringId, UUID requesterId);

    /**
     * Get attendance history for a student in an offering (Use Case: Student attendance history).
     * Returns all lessons for the offering (regardless of lesson status), with for each lesson:
     * lesson info, student's attendance record (if marked), and all absence notices for that lesson.
     * Includes summary: missed count (ABSENT/EXCUSED) and total absence notices submitted.
     * Batch-loaded; no N+1.
     *
     * @param studentId   student profile ID (must not be null)
     * @param offeringId  offering ID (must not be null)
     * @param requesterId current authenticated user ID (student can only view own; teacher/admin can view any)
     * @return aggregated DTO with student, subjectName, missedCount, absenceNoticesSubmittedCount, and lessons list
     * @throws com.example.interhubdev.error.AppException NOT_FOUND if offering or student not found,
     *         UNAUTHORIZED if requesterId is null, FORBIDDEN if requester cannot view (e.g. student viewing another)
     */
    StudentAttendanceHistoryDto getStudentAttendanceHistory(UUID studentId, UUID offeringId, UUID requesterId);

    /**
     * Get full homework history for a student in an offering (Use Case: Student homework history).
     * Returns all homework assignments for the offering and the student's submission (solution) and grade per assignment.
     * For frontend to display complete student homework info (e.g. student card). Batch-loaded; no N+1.
     *
     * @param studentId   student profile ID (must not be null)
     * @param offeringId  offering ID (must not be null)
     * @param requesterId current authenticated user ID (must have permission to view submissions and grades, e.g. teacher/admin)
     * @return aggregated DTO with student, subjectName, and items (homework + lesson + submission + grade + files per row)
     * @throws com.example.interhubdev.error.AppException NOT_FOUND if offering or student not found,
     *         UNAUTHORIZED if requesterId is null, FORBIDDEN if requester cannot view
     */
    StudentHomeworkHistoryDto getStudentHomeworkHistory(UUID studentId, UUID offeringId, UUID requesterId);
}
