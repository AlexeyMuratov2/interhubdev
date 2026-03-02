package com.example.interhubdev.absencenotice;

import java.time.LocalDateTime;

/**
 * Aggregated absence period for a student's notice.
 */
public record StudentNoticePeriodSummary(
        LocalDateTime startAt,
        LocalDateTime endAt
) {
}
