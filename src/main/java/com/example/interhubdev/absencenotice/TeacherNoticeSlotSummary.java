package com.example.interhubdev.absencenotice;

import java.time.LocalTime;
import java.util.UUID;

/**
 * Summary of an offering slot for display in teacher's absence notice list.
 */
public record TeacherNoticeSlotSummary(
        UUID id,
        UUID offeringId,
        int dayOfWeek,
        LocalTime startTime,
        LocalTime endTime,
        String lessonType,
        UUID roomId,
        UUID teacherId,
        UUID timeslotId
) {
}
