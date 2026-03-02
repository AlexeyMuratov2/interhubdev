package com.example.interhubdev.attendance;

import com.example.interhubdev.absencenotice.AbsenceNoticeDto;
import com.example.interhubdev.absencenotice.AbsenceNoticeStatus;
import com.example.interhubdev.absencenotice.StudentAbsenceNoticePage;
import com.example.interhubdev.absencenotice.SubmitAbsenceNoticeRequest;
import com.example.interhubdev.absencenotice.TeacherAbsenceNoticePage;
import com.example.interhubdev.attendancerecord.AttendanceRecordDto;
import com.example.interhubdev.attendancerecord.AttendanceStatus;
import com.example.interhubdev.attendancerecord.GroupAttendanceSummaryDto;
import com.example.interhubdev.attendancerecord.MarkAttendanceItem;
import com.example.interhubdev.attendancerecord.StudentAttendanceDto;
import com.example.interhubdev.error.AppException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Public API for Attendance module (facade): mark and query attendance records and absence notices.
 * Delegates to attendancerecord and absencenotice modules.
 */
public interface AttendanceApi {

    List<AttendanceRecordDto> markAttendanceBulk(UUID sessionId, List<MarkAttendanceItem> items, UUID markedBy);

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

    SessionAttendanceDto getSessionAttendance(UUID sessionId, UUID requesterId, boolean includeCanceled);

    /**
     * Get list of absence notices for a lesson session (teacher). Access checked via session.
     */
    List<AbsenceNoticeDto> getSessionNotices(UUID sessionId, UUID requesterId, boolean includeCanceled);

    StudentAttendanceDto getStudentAttendance(
            UUID studentId,
            LocalDateTime from,
            LocalDateTime to,
            UUID offeringId,
            UUID groupId,
            UUID requesterId
    );

    StudentAttendanceByLessonsDto getStudentAttendanceByLessonIds(
            UUID studentId,
            List<UUID> lessonIds,
            UUID requesterId
    );

    GroupAttendanceSummaryDto getGroupAttendanceSummary(
            UUID groupId,
            LocalDate from,
            LocalDate to,
            UUID offeringId,
            UUID requesterId
    );

    TeacherAbsenceNoticePage getTeacherAbsenceNotices(
            UUID teacherId,
            List<AbsenceNoticeStatus> statuses,
            UUID cursor,
            Integer limit
    );

    StudentAbsenceNoticePage getMyAbsenceNotices(
            UUID studentId,
            LocalDateTime from,
            LocalDateTime to,
            UUID cursor,
            Integer limit
    );

    AbsenceNoticeDto createAbsenceNotice(SubmitAbsenceNoticeRequest request, UUID studentId);

    AbsenceNoticeDto updateAbsenceNotice(UUID noticeId, SubmitAbsenceNoticeRequest request, UUID studentId);

    /**
     * Cancel an active (SUBMITTED) absence notice. Only the notice owner can cancel.
     * Also detaches the notice from any attendance records.
     */
    AbsenceNoticeDto cancelAbsenceNotice(UUID noticeId, UUID studentId);

    /**
     * Attach an absence notice to an attendance record (teacher only).
     */
    AttendanceRecordDto attachNoticeToRecord(UUID recordId, UUID noticeId, UUID requesterId);

    /**
     * Detach absence notice from an attendance record (teacher only).
     */
    AttendanceRecordDto detachNoticeFromRecord(UUID recordId, UUID requesterId);
}
