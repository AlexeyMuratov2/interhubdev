package com.example.interhubdev.schedule.internal;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

interface LessonRepository extends JpaRepository<Lesson, UUID> {

    List<Lesson> findByOfferingIdOrderByDateAscStartTimeAsc(UUID offeringId);

    List<Lesson> findByDateOrderByStartTimeAsc(LocalDate date);

    List<Lesson> findByOfferingIdAndDate(UUID offeringId, LocalDate date);

    boolean existsByOfferingIdAndDateAndStartTimeAndEndTime(UUID offeringId, LocalDate date, LocalTime startTime, LocalTime endTime);

    void deleteByOfferingId(UUID offeringId);

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
}
