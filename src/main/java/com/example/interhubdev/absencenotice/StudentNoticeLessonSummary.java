package com.example.interhubdev.absencenotice;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

/**
 * Summary of a lesson (session) for display in student's absence notice list.
 */
public record StudentNoticeLessonSummary(
        UUID id,
        UUID offeringId,
        LocalDate date,
        LocalTime startTime,
        LocalTime endTime,
        String topic,
        String status,
        String lessonType
) {
}
