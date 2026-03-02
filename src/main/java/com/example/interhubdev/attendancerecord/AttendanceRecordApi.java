package com.example.interhubdev.attendancerecord;

import com.example.interhubdev.error.AppException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Public API for attendance records: mark and query. Does not validate absence notices;
 * callers (facade or absence-notice) perform notice validation when attaching.
 */
public interface AttendanceRecordApi {

    /**
     * Mark attendance for multiple students in a lesson session (bulk).
     * All-or-nothing. Does not validate absenceNoticeId; caller must ensure it is valid if set.
     *
     * @param sessionId lesson session ID
     * @param items     list of items (studentId, status, optional minutesLate, teacherComment, optional absenceNoticeId)
     * @param markedBy  user ID who marks
     * @return list of created/updated record DTOs
     */
    List<AttendanceRecordDto> markAttendanceBulk(UUID sessionId, List<MarkAttendanceItem> items, UUID markedBy);

    /**
     * Mark attendance for a single student. Does not validate absenceNoticeId.
     *
     * @param sessionId       lesson session ID
     * @param studentId       student profile ID
     * @param status          attendance status
     * @param minutesLate     optional (only for LATE)
     * @param teacherComment  optional
     * @param absenceNoticeId optional; stored as-is without validation
     * @param markedBy        user ID who marks
     * @return created/updated record DTO
     */
    AttendanceRecordDto markAttendanceSingle(
            UUID sessionId,
            UUID studentId,
            AttendanceStatus status,
            Integer minutesLate,
            String teacherComment,
            UUID absenceNoticeId,
            UUID markedBy
    );

    /**
     * Get attendance records for a lesson session (records only, no notices list).
     *
     * @param sessionId   lesson session ID
     * @param requesterId current user ID (for authorization)
     * @return session records DTO with counts and student rows
     */
    SessionRecordsDto getSessionRecords(UUID sessionId, UUID requesterId);

    /**
     * Get attendance history for a student.
     *
     * @param studentId   student profile ID
     * @param from        optional filter: markedAt >= from
     * @param to          optional filter: markedAt <= to
     * @param offeringId  optional filter
     * @param groupId     optional filter
     * @param requesterId current user ID
     * @return student attendance DTO
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
     * Get attendance records for a student by lesson IDs (records only).
     *
     * @param studentId   student profile ID
     * @param lessonIds   list of lesson (session) IDs (order preserved)
     * @param requesterId current user ID
     * @return one item per lesson with optional record
     */
    StudentAttendanceRecordsByLessonsDto getStudentAttendanceByLessonIds(
            UUID studentId,
            List<UUID> lessonIds,
            UUID requesterId
    );

    /**
     * Get group attendance summary.
     *
     * @param groupId     group ID
     * @param from        optional date filter
     * @param to          optional date filter
     * @param offeringId  optional filter
     * @param requesterId current user ID
     * @return group summary DTO
     */
    GroupAttendanceSummaryDto getGroupAttendanceSummary(
            UUID groupId,
            LocalDate from,
            LocalDate to,
            UUID offeringId,
            UUID requesterId
    );

    /**
     * Attach an absence notice to a record (sets attendance_record.absence_notice_id only).
     * No validation of notice existence or match.
     *
     * @param recordId attendance record ID
     * @param noticeId  absence notice ID to set
     * @param requesterId user ID (for authorization)
     * @return updated record DTO
     * @throws AppException NOT_FOUND if record not found, FORBIDDEN if not allowed
     */
    AttendanceRecordDto attachNotice(UUID recordId, UUID noticeId, UUID requesterId);

    /**
     * Detach absence notice from a record (clears absence_notice_id).
     *
     * @param recordId    attendance record ID
     * @param requesterId user ID (for authorization)
     * @return updated record DTO
     * @throws AppException NOT_FOUND if record not found, FORBIDDEN if not allowed
     */
    AttendanceRecordDto detachNotice(UUID recordId, UUID requesterId);

    /**
     * Find a single record by ID (for port callers that need record details).
     *
     * @param recordId attendance record ID
     * @return record DTO if found
     */
    java.util.Optional<AttendanceRecordDto> findRecordById(UUID recordId);

    /**
     * Detach absence notice from all records that reference it (e.g. when student cancels notice).
     *
     * @param noticeId absence notice ID
     */
    void detachNoticeByNoticeId(UUID noticeId);

    /**
     * Detach absence notice from records of a specific lesson session.
     *
     * @param noticeId  absence notice ID
     * @param sessionId lesson session ID
     */
    void detachNoticeByNoticeIdAndSessionId(UUID noticeId, UUID sessionId);
}
