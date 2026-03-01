package com.example.interhubdev.absencenotice;

/**
 * Enriched absence notice item for student's list: notice plus lesson, offering, and slot context.
 */
public record StudentAbsenceNoticeItemDto(
        AbsenceNoticeDto notice,
        StudentNoticeLessonSummary lesson,
        StudentNoticeOfferingSummary offering,
        StudentNoticeSlotSummary slot
) {
}
