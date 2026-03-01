package com.example.interhubdev.absencenotice;

/**
 * Enriched absence notice item for teacher's list: notice plus student, lesson, offering, slot, and group context.
 */
public record TeacherAbsenceNoticeItemDto(
        AbsenceNoticeDto notice,
        TeacherNoticeStudentSummary student,
        TeacherNoticeLessonSummary lesson,
        TeacherNoticeOfferingSummary offering,
        TeacherNoticeSlotSummary slot,
        TeacherNoticeGroupSummary group
) {
}
