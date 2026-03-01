package com.example.interhubdev.absencenotice.internal;

/**
 * Event type constants for absence notice integration events.
 */
public final class AbsenceNoticeEventTypes {

    private AbsenceNoticeEventTypes() {
    }

    public static final String ABSENCE_NOTICE_SUBMITTED = "attendance.absence_notice.submitted";
    public static final String ABSENCE_NOTICE_UPDATED = "attendance.absence_notice.updated";
    public static final String ABSENCE_NOTICE_CANCELED = "attendance.absence_notice.canceled";
    public static final String ABSENCE_NOTICE_ATTACHED = "attendance.record.notice_attached";
    public static final String ABSENCE_NOTICE_TEACHER_RESPONDED = "attendance.absence_notice.teacher_responded";
}
