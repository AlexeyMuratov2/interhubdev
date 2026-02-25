package com.example.interhubdev.attendance;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

/**
 * Summary of a lesson (session) for display in student's absence notice list.
 * lessonType indicates the kind of class (e.g. lecture, seminar) for UI display.
 */
public record StudentNoticeLessonSummary(
        UUID id,
        UUID offeringId,
        LocalDate date,
        LocalTime startTime,
        LocalTime endTime,
        String topic,
        String status,
        /** Type of lesson: LECTURE, PRACTICE, LAB, SEMINAR. Null if slot not linked or legacy lesson. */
        String lessonType
) {}
