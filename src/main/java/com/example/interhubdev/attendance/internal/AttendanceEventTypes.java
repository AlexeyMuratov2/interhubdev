package com.example.interhubdev.attendance.internal;

/**
 * Event type constants for Attendance module integration events.
 * <p>
 * These constants define the event types published to the outbox when attendance-related
 * domain events occur. Event types use dot notation (e.g., "attendance.absence_notice.submitted").
 */
public final class AttendanceEventTypes {

    private AttendanceEventTypes() {
        // Utility class, no instantiation
    }

    /**
     * Event type when a student submits a new absence notice.
     */
    public static final String ABSENCE_NOTICE_SUBMITTED = "attendance.absence_notice.submitted";

    /**
     * Event type when a student updates an existing absence notice.
     */
    public static final String ABSENCE_NOTICE_UPDATED = "attendance.absence_notice.updated";

    /**
     * Event type when a student cancels an absence notice.
     */
    public static final String ABSENCE_NOTICE_CANCELED = "attendance.absence_notice.canceled";

    /**
     * Event type when attendance is marked for a student (bulk or single).
     */
    public static final String ATTENDANCE_MARKED = "attendance.record.marked";

    /**
     * Event type when an absence notice is attached to an attendance record.
     */
    public static final String ABSENCE_NOTICE_ATTACHED = "attendance.record.notice_attached";
}
