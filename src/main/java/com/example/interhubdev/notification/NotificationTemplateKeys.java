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
     * Params: sessionIds (list of UUID strings), noticeId, studentId, noticeType (ABSENT|LATE),
     * studentName (display name), periodStart (ISO-8601), periodEnd (ISO-8601).
     * <p>
     * Data: route="sessionAttendance", sessionId (first session), focus="notices", noticeId, studentId
     */
    public static final String ABSENCE_NOTICE_SUBMITTED = "attendance.absenceNotice.submitted";

    /**
     * Template key when a student updates an existing absence notice.
     * <p>
     * Params: sessionIds (list of UUID strings), noticeId, studentId, noticeType (ABSENT|LATE),
     * studentName (display name), periodStart (ISO-8601), periodEnd (ISO-8601).
     * <p>
     * Data: route="sessionAttendance", sessionId (first session), focus="notices", noticeId, studentId
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

    /**
     * Template key when a student submits a homework solution.
     * <p>
     * Params: lessonId (UUID string), lessonDisplay (e.g. date + time or slot), subjectName, studentName (display name).
     * <p>
     * Data: route (e.g. lesson homework submissions), lessonId, homeworkId, submissionId, studentId (authorId)
     */
    public static final String HOMEWORK_SUBMISSION_SUBMITTED = "submission.homeworkSubmission.submitted";

    /**
     * Template key when a lesson is rescheduled (date/time changed).
     * <p>
     * Params: lessonId, subjectName, oldDateTime (e.g. "2025-03-01 10:00–11:30"), newDateTime, offeringId.
     * <p>
     * Data: route (e.g. schedule/lesson), lessonId, offeringId
     */
    public static final String LESSON_RESCHEDULED = "schedule.lesson.rescheduled";

    /**
     * Template key when a lesson is deleted.
     * <p>
     * Params: lessonId, subjectName, lessonDate (date of the removed lesson), offeringId.
     * <p>
     * Data: route (e.g. schedule), lessonId, offeringId
     */
    public static final String LESSON_DELETED = "schedule.lesson.deleted";
}
