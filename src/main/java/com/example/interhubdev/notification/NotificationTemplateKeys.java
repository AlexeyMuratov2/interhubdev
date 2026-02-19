package com.example.interhubdev.notification;

/**
 * Template key constants for notifications.
 * <p>
 * These keys are used by the frontend to render localized notification text.
 * Backend stores only the key and params/data; frontend handles localization.
 */
public final class NotificationTemplateKeys {

    private NotificationTemplateKeys() {
        // Utility class, no instantiation
    }

    /**
     * Template key when a student submits a new absence notice.
     * <p>
     * Params: sessionId (UUID string), noticeId (UUID string), studentId (UUID string), noticeType (ABSENT|LATE)
     * <p>
     * Data: route="sessionAttendance", sessionId, focus="notices", noticeId, studentId
     */
    public static final String ABSENCE_NOTICE_SUBMITTED = "attendance.absenceNotice.submitted";

    /**
     * Template key when a student updates an existing absence notice.
     * <p>
     * Params: sessionId (UUID string), noticeId (UUID string), studentId (UUID string), noticeType (ABSENT|LATE)
     * <p>
     * Data: route="sessionAttendance", sessionId, focus="notices", noticeId, studentId
     */
    public static final String ABSENCE_NOTICE_UPDATED = "attendance.absenceNotice.updated";

    /**
     * Template key when attendance is marked for a student.
     * <p>
     * Params: sessionId (UUID string), recordId (UUID string), status (PRESENT|ABSENT|LATE|EXCUSED)
     * <p>
     * Data: route="studentAttendance", sessionId, recordId
     */
    public static final String ATTENDANCE_MARKED = "attendance.record.marked";
}
