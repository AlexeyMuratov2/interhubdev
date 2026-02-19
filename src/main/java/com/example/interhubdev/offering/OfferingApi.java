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

    /**
     * Get offering by ID.
     *
     * @param id offering ID
     * @return optional offering DTO if found
     */
    Optional<GroupSubjectOfferingDto> findOfferingById(UUID id);

    /**
     * Get all offerings for a group.
     *
     * @param groupId group ID
     * @return list of offerings for the group, ordered by curriculum subject
     */
    List<GroupSubjectOfferingDto> findOfferingsByGroupId(UUID groupId);

    /**
     * Get all offerings where teacher is assigned (as main teacher, slot teacher, or offering teacher).
     *
     * @param teacherId teacher entity ID
     * @return list of offerings for the teacher; empty if teacher has no offerings
     */
    List<GroupSubjectOfferingDto> findOfferingsByTeacherId(UUID teacherId);

    /**
     * Create a new group subject offering.
     *
     * @param groupId group ID
     * @param curriculumSubjectId curriculum subject ID
     * @param teacherId optional main teacher ID
     * @param roomId optional default room ID
     * @param format optional format (offline, online, mixed)
     * @param notes optional notes
     * @return created offering DTO
     * @throws com.example.interhubdev.error.AppException NOT_FOUND if group, curriculum subject, teacher or room not found;
     *         CONFLICT if offering already exists for group and curriculum subject
     */
    GroupSubjectOfferingDto createOffering(
            UUID groupId,
            UUID curriculumSubjectId,
            UUID teacherId,
            UUID roomId,
            String format,
            String notes
    );

    /**
     * Update an existing offering.
     *
     * @param id offering ID
     * @param teacherId optional main teacher ID
     * @param roomId optional default room ID
     * @param format optional format (offline, online, mixed)
     * @param notes optional notes
     * @return updated offering DTO
     * @throws com.example.interhubdev.error.AppException NOT_FOUND if offering, teacher or room not found
     */
    GroupSubjectOfferingDto updateOffering(
            UUID id,
            UUID teacherId,
            UUID roomId,
            String format,
            String notes
    );

    /**
     * Delete an offering by ID.
     *
     * @param id offering ID
     * @throws com.example.interhubdev.error.AppException NOT_FOUND if offering not found
     */
    void deleteOffering(UUID id);

    // --- Offering Teachers ---

    /**
     * Get all teachers assigned to an offering (derived from main teacher and slot teachers).
     * Main teacher has role null; slot teachers have role = slot's lessonType (LECTURE, PRACTICE, LAB).
     *
     * @param offeringId offering ID
     * @return list of offering teacher items (teacherId, role)
     * @throws com.example.interhubdev.error.AppException NOT_FOUND if offering not found
     */
    List<OfferingTeacherItemDto> findTeachersByOfferingId(UUID offeringId);

    // --- Offering Slots (weekly recurring timeslots) ---

    /**
     * Get all weekly slots for an offering.
     *
     * @param offeringId offering ID
     * @return list of offering slots ordered by lesson type
     */
    List<OfferingSlotDto> findSlotsByOfferingId(UUID offeringId);

    /**
     * Add a weekly slot to an offering. Slot owns day and time.
     * Either timeslotId (copy time from template) or (dayOfWeek, startTime, endTime) must be provided.
     *
     * @param offeringId offering ID
     * @param timeslotId optional; if set, day/time are copied from this timeslot (UI hint)
     * @param dayOfWeek required if timeslotId null (1..7)
     * @param startTime required if timeslotId null
     * @param endTime required if timeslotId null
     * @param lessonType LECTURE, PRACTICE, LAB, or SEMINAR
     * @param roomId optional room override
     * @param teacherId optional teacher override
     * @return the created slot DTO
     * @throws com.example.interhubdev.error.AppException NOT_FOUND if offering, timeslot or teacher not found;
     *         BAD_REQUEST if neither timeslotId nor (dayOfWeek, startTime, endTime) provided or invalid;
     *         CONFLICT if slot already exists for same day, time and lesson type
     */
    OfferingSlotDto addOfferingSlot(UUID offeringId, UUID timeslotId, Integer dayOfWeek,
                                   java.time.LocalTime startTime, java.time.LocalTime endTime,
                                   String lessonType, UUID roomId, UUID teacherId);

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
     * @throws com.example.interhubdev.error.AppException NOT_FOUND if semester not found
     */
    int generateLessonsForGroup(UUID groupId, UUID semesterId);

    /**
     * Delete existing lessons for the offering and regenerate them.
     *
     * @param offeringId offering ID
     * @param semesterId semester ID
     * @return number of lessons created
     * @throws com.example.interhubdev.error.AppException NOT_FOUND if offering or semester not found;
     *         BAD_REQUEST if offering has no slots
     */
    int regenerateLessonsForOffering(UUID offeringId, UUID semesterId);
}
