package com.example.interhubdev.attendance;

/**
 * Status of an absence notice.
 */
public enum AbsenceNoticeStatus {
    /**
     * Notice has been submitted and is active.
     */
    SUBMITTED,

    /**
     * Notice has been canceled by the student.
     */
    CANCELED,

    /**
     * Notice has been acknowledged by teacher (optional, reserved for future use).
     */
    ACKNOWLEDGED,

    /**
     * Notice has been attached to an attendance record (reserved for future use).
     */
    ATTACHED
}
