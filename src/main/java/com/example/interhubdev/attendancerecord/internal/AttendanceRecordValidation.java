package com.example.interhubdev.attendancerecord.internal;

import com.example.interhubdev.attendancerecord.AttendanceStatus;

/**
 * Business validation for attendance records.
 */
final class AttendanceRecordValidation {

    private static final int MAX_TEACHER_COMMENT_LENGTH = 2000;

    private AttendanceRecordValidation() {
    }

    static void validateStatusAndMinutesLate(AttendanceStatus status, Integer minutesLate) {
        if (status == AttendanceStatus.LATE) {
            if (minutesLate != null && minutesLate < 0) {
                throw AttendanceRecordErrors.validationFailed("minutesLate must be >= 0");
            }
        } else {
            if (minutesLate != null) {
                throw AttendanceRecordErrors.validationFailed("minutesLate must be null when status is not LATE");
            }
        }
    }

    static void validateTeacherComment(String teacherComment) {
        if (teacherComment != null && teacherComment.length() > MAX_TEACHER_COMMENT_LENGTH) {
            throw AttendanceRecordErrors.validationFailed(
                    "teacherComment must not exceed " + MAX_TEACHER_COMMENT_LENGTH + " characters");
        }
    }
}
