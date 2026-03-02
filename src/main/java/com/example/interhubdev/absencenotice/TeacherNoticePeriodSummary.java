package com.example.interhubdev.absencenotice;

import java.time.LocalDateTime;

/**
 * Date range for a teacher's absence notice (valid from startAt to endAt).
 * Used when a notice covers multiple lessons — only the period is returned, not lesson/offering/group.
 */
public record TeacherNoticePeriodSummary(
        LocalDateTime startAt,
        LocalDateTime endAt
) {
}
