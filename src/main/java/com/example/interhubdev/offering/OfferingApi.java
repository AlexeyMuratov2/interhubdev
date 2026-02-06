package com.example.interhubdev.offering;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Public API for Offering module: group subject offerings, offering teachers,
 * offering weekly slots, and lesson generation.
 */
public interface OfferingApi {

    // --- Offering CRUD ---

    Optional<GroupSubjectOfferingDto> findOfferingById(UUID id);

    List<GroupSubjectOfferingDto> findOfferingsByGroupId(UUID groupId);

    GroupSubjectOfferingDto createOffering(
            UUID groupId,
            UUID curriculumSubjectId,
            UUID teacherId,
            UUID roomId,
            String format,
            String notes
    );

    GroupSubjectOfferingDto updateOffering(
            UUID id,
            UUID teacherId,
            UUID roomId,
            String format,
            String notes
    );

    void deleteOffering(UUID id);

    // --- Offering Teachers ---

    List<OfferingTeacherDto> findTeachersByOfferingId(UUID offeringId);

    OfferingTeacherDto addOfferingTeacher(UUID offeringId, UUID teacherId, String role);

    void removeOfferingTeacher(UUID id);

    // --- Offering Slots (weekly recurring timeslots) ---

    /**
     * Get all weekly slots for an offering.
     *
     * @param offeringId offering ID
     * @return list of offering slots ordered by lesson type
     */
    List<OfferingSlotDto> findSlotsByOfferingId(UUID offeringId);

    /**
     * Add a weekly slot to an offering (e.g., "LECTURE on Monday slot 2").
     *
     * @param offeringId offering ID
     * @param timeslotId timeslot ID (day of week + time)
     * @param lessonType lesson type: LECTURE, PRACTICE, LAB, or SEMINAR
     * @param roomId optional room override (null = use offering's default room)
     * @param teacherId optional teacher override (null = use offering's default teacher)
     * @return the created slot DTO
     * @throws com.example.interhubdev.error.AppException NOT_FOUND if offering/timeslot/teacher not found;
     *         CONFLICT if slot already exists for this offering+timeslot+type;
     *         BAD_REQUEST if lesson type is invalid
     */
    OfferingSlotDto addOfferingSlot(UUID offeringId, UUID timeslotId, String lessonType, UUID roomId, UUID teacherId);

    /**
     * Remove a weekly slot from an offering.
     *
     * @param id slot ID
     * @throws com.example.interhubdev.error.AppException NOT_FOUND if slot not found
     */
    void removeOfferingSlot(UUID id);

    // --- Lesson Generation ---

    /**
     * Generate lessons for a single offering across the given semester.
     * Uses the offering's weekly slots and the curriculum subject's durationWeeks.
     *
     * @param offeringId offering ID
     * @param semesterId semester ID (determines start/end dates)
     * @return number of lessons created
     * @throws com.example.interhubdev.error.AppException if offering/semester/curriculum subject not found,
     *         or offering has no slots, or lessons already exist
     */
    int generateLessonsForOffering(UUID offeringId, UUID semesterId);

    /**
     * Generate lessons for all offerings of a group across the given semester.
     *
     * @param groupId group ID
     * @param semesterId semester ID
     * @return total number of lessons created across all offerings
     */
    int generateLessonsForGroup(UUID groupId, UUID semesterId);

    /**
     * Delete existing lessons for the offering and regenerate them.
     *
     * @param offeringId offering ID
     * @param semesterId semester ID
     * @return number of lessons created
     */
    int regenerateLessonsForOffering(UUID offeringId, UUID semesterId);
}
