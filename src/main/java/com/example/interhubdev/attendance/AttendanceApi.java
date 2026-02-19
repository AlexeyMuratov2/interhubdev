package com.example.interhubdev.attendance;

import com.example.interhubdev.error.AppException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Public API for Attendance module: mark and query attendance records.
 * All errors are thrown as {@link AppException} and handled by global exception handler.
 */
public interface AttendanceApi {

    /**
     * Mark attendance for multiple students in a lesson session (bulk operation).
     * All-or-nothing transaction: if any item fails validation, entire batch is rolled back.
     *
     * @param sessionId lesson session (lesson) ID
     * @param items     list of attendance items (studentId, status, optional minutesLate, optional teacherComment)
     * @param markedBy  user ID who marks attendance (must be teacher of session or admin)
     * @return list of created/updated attendance record DTOs
     * @throws AppException NOT_FOUND (session/student), BAD_REQUEST (validation), FORBIDDEN (not teacher of session)
     */
    List<AttendanceRecordDto> markAttendanceBulk(UUID sessionId, List<MarkAttendanceItem> items, UUID markedBy);

    /**
     * Mark attendance for a single student in a lesson session.
     *
     * @param sessionId    lesson session (lesson) ID
     * @param studentId    student profile ID
     * @param status       attendance status (required)
     * @param minutesLate  optional minutes late (required if status=LATE, must be null otherwise)
     * @param teacherComment optional teacher comment
     * @param absenceNoticeId optional explicit absence notice ID to attach
     * @param autoAttachLastNotice if true, automatically attach last submitted notice for this student and session
     * @param markedBy     user ID who marks attendance (must be teacher of session or admin)
     * @return created/updated attendance record DTO
     * @throws AppException NOT_FOUND (session/student), BAD_REQUEST (validation), FORBIDDEN (not teacher of session)
     */
    AttendanceRecordDto markAttendanceSingle(
            UUID sessionId,
            UUID studentId,
            AttendanceStatus status,
            Integer minutesLate,
            String teacherComment,
            UUID absenceNoticeId,
            Boolean autoAttachLastNotice,
            UUID markedBy
    );

    /**
     * Get attendance records for a lesson session.
     * Returns roster of students with their attendance status (UNMARKED if no record exists).
     *
     * @param sessionId  lesson session (lesson) ID
     * @param requesterId current user ID (must be teacher of session or admin)
     * @param includeCanceled if true, include CANCELED notices in student notices; if false, only SUBMITTED
     * @return session attendance DTO with counts and student records (including notices per student)
     * @throws AppException NOT_FOUND (session), FORBIDDEN (not teacher of session)
     */
    SessionAttendanceDto getSessionAttendance(UUID sessionId, UUID requesterId, boolean includeCanceled);

    /**
     * Get attendance history for a student.
     *
     * @param studentId   student profile ID
     * @param from        optional filter: markedAt >= from
     * @param to          optional filter: markedAt <= to
     * @param offeringId  optional filter: only records for this offering
     * @param groupId     optional filter: only records for sessions in this group
     * @param requesterId current user ID (must be student themselves or teacher/admin)
     * @return student attendance DTO with summary and records
     * @throws AppException NOT_FOUND (student), FORBIDDEN (student can only view own records)
     */
    StudentAttendanceDto getStudentAttendance(
            UUID studentId,
            LocalDateTime from,
            LocalDateTime to,
            UUID offeringId,
            UUID groupId,
            UUID requesterId
    );

    /**
     * Get attendance summary for a group (per-student counts).
     *
     * @param groupId     student group ID
     * @param from        optional filter: session date >= from
     * @param to          optional filter: session date <= to
     * @param offeringId optional filter: only sessions for this offering
     * @param requesterId current user ID (must be teacher of group or admin)
     * @return group attendance summary DTO with per-student counts
     * @throws AppException NOT_FOUND (group), FORBIDDEN (not teacher of group)
     */
    GroupAttendanceSummaryDto getGroupAttendanceSummary(
            UUID groupId,
            LocalDate from,
            LocalDate to,
            UUID offeringId,
            UUID requesterId
    );

}
