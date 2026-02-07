package com.example.interhubdev.offering;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

/**
 * Port for creating and deleting lessons in the Schedule module.
 * Used by the Offering module's lesson generation logic.
 * <p>
 * Implemented by an adapter in the adapter package using ScheduleApi.
 */
public interface LessonCreationPort {

    /**
     * Command to create a single lesson. Lesson owns time (startTime, endTime).
     * timeslotId is optional (UI hint). offeringSlotId references the offering slot this lesson was generated from.
     */
    record LessonCreateCommand(
            UUID offeringId,
            UUID offeringSlotId,
            LocalDate date,
            LocalTime startTime,
            LocalTime endTime,
            UUID timeslotId,
            UUID roomId,
            String status
    ) {}

    /**
     * Create multiple lessons in a single batch operation.
     *
     * @param commands list of lesson creation commands
     * @return number of lessons actually created
     */
    int createLessonsInBulk(List<LessonCreateCommand> commands);

    /**
     * Delete all lessons belonging to a specific offering.
     */
    void deleteLessonsByOfferingId(UUID offeringId);

    /**
     * Delete lessons for an offering whose date is within the given range (inclusive).
     * Used when regenerating lessons for a single semester so lessons of other semesters are preserved.
     *
     * @param offeringId offering ID
     * @param startInclusive semester start date (inclusive)
     * @param endInclusive semester end date (inclusive)
     */
    void deleteLessonsByOfferingIdAndDateRange(UUID offeringId, LocalDate startInclusive, LocalDate endInclusive);

    /**
     * Delete all lessons that reference the given offering slot (generated from that slot).
     * Used when removing an offering slot.
     *
     * @param offeringSlotId offering slot ID
     */
    void deleteLessonsByOfferingSlotId(UUID offeringSlotId);

    /**
     * Delete lessons for an offering that match the given weekly slot (day of week and time).
     * Used when removing an offering slot (for legacy lessons without offeringSlotId).
     *
     * @param offeringId offering ID
     * @param dayOfWeek  day of week (1–7, Monday=1)
     * @param startTime  slot start time
     * @param endTime    slot end time
     */
    void deleteLessonsByOfferingIdAndDayOfWeekAndStartTimeAndEndTime(
            UUID offeringId, int dayOfWeek, java.time.LocalTime startTime, java.time.LocalTime endTime);
}
