package com.example.interhubdev.attendance;

/**
 * Attendance status for a student in a lesson session.
 */
public enum AttendanceStatus {
    /**
     * Student was present.
     */
    PRESENT,

    /**
     * Student was absent.
     */
    ABSENT,

    /**
     * Student was late (optional minutesLate >= 0 if provided).
     */
    LATE,

    /**
     * Student was absent with valid excuse (teacherComment optional).
     */
    EXCUSED
}
