package com.example.interhubdev.attendance.internal;

import com.example.interhubdev.attendance.AttendanceStatus;

/**
 * Business validation for attendance records. Throws {@link com.example.interhubdev.error.AppException} via AttendanceErrors.
 */
final class AttendanceValidation {

    private static final int MAX_TEACHER_COMMENT_LENGTH = 2000;

    private AttendanceValidation() {
    }

    /**
     * Validate status and minutesLate combination:
     * - LATE: minutesLate is optional; if provided, must be >= 0
     * - Non-LATE: minutesLate must be null
     */
    static void validateStatusAndMinutesLate(AttendanceStatus status, Integer minutesLate) {
        if (status == AttendanceStatus.LATE) {
            if (minutesLate != null && minutesLate < 0) {
                throw AttendanceErrors.validationFailed("minutesLate must be >= 0");
            }
        } else {
            if (minutesLate != null) {
                throw AttendanceErrors.validationFailed("minutesLate must be null when status is not LATE");
            }
        }
    }

    /**
     * Validate teacher comment length.
     */
    static void validateTeacherComment(String teacherComment) {
        if (teacherComment != null && teacherComment.length() > MAX_TEACHER_COMMENT_LENGTH) {
            throw AttendanceErrors.validationFailed(
                    "teacherComment must not exceed " + MAX_TEACHER_COMMENT_LENGTH + " characters");
        }
    }
}
