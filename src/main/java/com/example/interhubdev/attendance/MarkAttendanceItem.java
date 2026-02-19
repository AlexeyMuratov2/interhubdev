package com.example.interhubdev.attendance;

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
         * If provided, this notice will be attached (must match session/student and be SUBMITTED).
         * Mutually exclusive with autoAttachLastNotice.
         */
        UUID absenceNoticeId,
        /**
         * If true, automatically attach the last submitted notice for this student and session.
         * Mutually exclusive with absenceNoticeId.
         */
        Boolean autoAttachLastNotice
) {
}
