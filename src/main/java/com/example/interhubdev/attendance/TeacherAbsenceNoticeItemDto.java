package com.example.interhubdev.attendance;

/**
 * Enriched absence notice item for teacher's list: notice plus student, lesson, offering, slot, and group context.
 * Includes subject (in offering) and offering_slot so the frontend has enough data for a good UI without extra requests.
 * Summary fields may be null if the related entity was not found (e.g. deleted student/group/slot).
 */
public record TeacherAbsenceNoticeItemDto(
        AbsenceNoticeDto notice,
        TeacherNoticeStudentSummary student,
        TeacherNoticeLessonSummary lesson,
        TeacherNoticeOfferingSummary offering,
        TeacherNoticeSlotSummary slot,
        TeacherNoticeGroupSummary group
) {}
