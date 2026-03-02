package com.example.interhubdev.attendance;

import com.example.interhubdev.absencenotice.AbsenceNoticeDto;
import com.example.interhubdev.absencenotice.SubmitAbsenceNoticeRequest;
import com.example.interhubdev.attendancerecord.AttendanceRecordDto;
import com.example.interhubdev.attendancerecord.AttendanceStatus;
import com.example.interhubdev.attendancerecord.MarkAttendanceItem;

import java.util.List;
import java.util.UUID;

/**
 * Public API for Attendance module (orchestrator): mark attendance and manage absence notices.
 * Coordinates write operations between attendancerecord and absencenotice. Read/query use composition or domain APIs.
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

    AbsenceNoticeDto createAbsenceNotice(SubmitAbsenceNoticeRequest request, UUID studentId);

    AbsenceNoticeDto updateAbsenceNotice(UUID noticeId, SubmitAbsenceNoticeRequest request, UUID studentId);

    /**
     * Cancel an active (SUBMITTED) absence notice. Only the notice owner can cancel.
     * Also detaches the notice from any attendance records.
     */
    AbsenceNoticeDto cancelAbsenceNotice(UUID noticeId, UUID studentId);

    /**
     * Remove one lesson from an active absence notice owned by the current student.
     * If it was the last lesson, the notice becomes canceled.
     */
    AbsenceNoticeDto removeLessonFromAbsenceNotice(UUID noticeId, UUID lessonSessionId, UUID studentId);

    /**
     * Attach an absence notice to an attendance record (teacher only).
     */
    AttendanceRecordDto attachNoticeToRecord(UUID recordId, UUID noticeId, UUID requesterId);

    /**
     * Detach absence notice from an attendance record (teacher only).
     */
    AttendanceRecordDto detachNoticeFromRecord(UUID recordId, UUID requesterId);
}
