package com.example.interhubdev.attendance;

/**
 * Enriched absence notice item for student's list: notice plus lesson, offering, and slot context.
 * Includes subject (in offering) and offering_slot so the frontend has enough data for a good UI without extra requests.
 * Summary fields may be null if the related entity was not found (e.g. deleted slot/offering).
 */
public record StudentAbsenceNoticeItemDto(
        AbsenceNoticeDto notice,
        StudentNoticeLessonSummary lesson,
        StudentNoticeOfferingSummary offering,
        StudentNoticeSlotSummary slot
) {}
