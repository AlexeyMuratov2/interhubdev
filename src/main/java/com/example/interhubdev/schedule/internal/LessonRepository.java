package com.example.interhubdev.schedule.internal;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;

interface LessonRepository extends JpaRepository<Lesson, UUID> {

    List<Lesson> findByOfferingIdOrderByDateAscStartTimeAsc(UUID offeringId);

    List<Lesson> findByDateOrderByStartTimeAsc(LocalDate date);

    /**
     * Lessons whose date is in [start, end] (inclusive). For week schedule: one query, no N+1.
     */
    List<Lesson> findByDateBetweenOrderByDateAscStartTimeAsc(LocalDate start, LocalDate end);

    /**
     * Lessons in [start, end] for given offerings. For week schedule by group: one query, no N+1.
     * Call only when offeringIds is non-empty (empty IN clause may be invalid in some DBs).
     */
    List<Lesson> findByDateBetweenAndOfferingIdInOrderByDateAscStartTimeAsc(LocalDate start, LocalDate end, List<UUID> offeringIds);

    List<Lesson> findByOfferingIdAndDate(UUID offeringId, LocalDate date);

    List<Lesson> findByDateAndOfferingIdInOrderByStartTimeAsc(LocalDate date, List<UUID> offeringIds);

    boolean existsByOfferingIdAndDateAndStartTimeAndEndTime(UUID offeringId, LocalDate date, LocalTime startTime, LocalTime endTime);

    void deleteByOfferingId(UUID offeringId);

    /**
     * Delete lessons for an offering whose date is within the given range (inclusive).
     * Used when regenerating lessons for a single semester so other semesters are not affected.
     */
    @Modifying
    @Query("DELETE FROM Lesson l WHERE l.offeringId = :offeringId AND l.date >= :start AND l.date <= :end")
    int deleteByOfferingIdAndDateBetween(UUID offeringId, LocalDate start, LocalDate end);

    void deleteByOfferingSlotId(UUID offeringSlotId);

    /**
     * Sets timeslotId to null for all lessons that reference the given timeslot (so slot can be deleted without removing lessons).
     */
    @Modifying
    @Query("UPDATE Lesson l SET l.timeslotId = null WHERE l.timeslotId = :timeslotId")
    int clearTimeslotReference(UUID timeslotId);

    /**
     * Sets timeslotId to null for all lessons that have a timeslot reference (before deleting all timeslots).
     */
    @Modifying
    @Query("UPDATE Lesson l SET l.timeslotId = null WHERE l.timeslotId IS NOT NULL")
    int clearAllTimeslotReferences();

    /**
     * Distinct offering_slot_id values that have at least one lesson, from the given set of slot IDs.
     * Used to filter teacher slots to only those with actual lessons (no empty slots).
     * Do not call with empty collection.
     */
    @Query("SELECT DISTINCT l.offeringSlotId FROM Lesson l WHERE l.offeringSlotId IS NOT NULL AND l.offeringSlotId IN :ids")
    Set<UUID> findDistinctOfferingSlotIdsByOfferingSlotIdIn(Collection<UUID> ids);
}
