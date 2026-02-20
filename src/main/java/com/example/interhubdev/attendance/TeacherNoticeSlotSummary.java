package com.example.interhubdev.attendance;

import java.time.LocalTime;
import java.util.UUID;

/**
 * Summary of an offering slot for display in teacher's absence notice list.
 * Contains the weekly slot (day, time, type, room, teacher) so the frontend can render
 * "which slot this lesson belongs to" without extra requests.
 */
public record TeacherNoticeSlotSummary(
        UUID id,
        UUID offeringId,
        /** Day of week (1 = Monday .. 7 = Sunday). */
        int dayOfWeek,
        LocalTime startTime,
        LocalTime endTime,
        /** LECTURE, PRACTICE, LAB, SEMINAR. */
        String lessonType,
        UUID roomId,
        UUID teacherId,
        UUID timeslotId
) {}
