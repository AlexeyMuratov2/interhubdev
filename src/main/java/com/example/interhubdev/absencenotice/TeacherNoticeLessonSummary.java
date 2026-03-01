package com.example.interhubdev.absencenotice;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

/**
 * Summary of a lesson for display in teacher's absence notice list.
 */
public record TeacherNoticeLessonSummary(
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
