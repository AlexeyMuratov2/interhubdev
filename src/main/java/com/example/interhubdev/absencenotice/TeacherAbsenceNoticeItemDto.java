package com.example.interhubdev.absencenotice;

/**
 * Enriched absence notice item for teacher's list.
 * <ul>
 *   <li>Single lesson: {@code lesson}, {@code offering}, {@code slot}, {@code group} and {@code period} are set.</li>
 *   <li>Multiple lessons: only {@code period} (valid from/to) is set; lesson/offering/slot/group are null.</li>
 * </ul>
 */
public record TeacherAbsenceNoticeItemDto(
        AbsenceNoticeDto notice,
        TeacherNoticeStudentSummary student,
        TeacherNoticePeriodSummary period,
        TeacherNoticeLessonSummary lesson,
        TeacherNoticeOfferingSummary offering,
        TeacherNoticeSlotSummary slot,
        TeacherNoticeGroupSummary group
) {
}
