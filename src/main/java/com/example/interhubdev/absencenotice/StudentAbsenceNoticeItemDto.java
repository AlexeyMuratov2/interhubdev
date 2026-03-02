package com.example.interhubdev.absencenotice;

/**
 * Enriched absence notice item for student's list: notice plus aggregated absence period.
 */
public record StudentAbsenceNoticeItemDto(
        AbsenceNoticeDto notice,
        StudentNoticePeriodSummary period
) {
}
