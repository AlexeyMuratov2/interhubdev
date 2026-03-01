package com.example.interhubdev.attendancerecord;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/**
 * Item for bulk attendance marking.
 */
public record MarkAttendanceItem(
        @NotNull(message = "studentId is required")
        UUID studentId,
        @NotNull(message = "status is required")
        AttendanceStatus status,
        Integer minutesLate,
        @Size(max = 2000, message = "teacherComment must not exceed 2000 characters")
        String teacherComment,
        /**
         * Optional explicit absence notice ID to attach to this record.
         * Validation of notice (session/student match, status) is done by caller (facade or absence-notice).
         */
        UUID absenceNoticeId,
        /**
         * If true, caller should resolve last submitted notice for this student and session and pass via absenceNoticeId.
         */
        Boolean autoAttachLastNotice
) {
}
