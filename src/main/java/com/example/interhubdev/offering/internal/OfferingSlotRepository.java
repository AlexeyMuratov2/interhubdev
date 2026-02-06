package com.example.interhubdev.offering.internal;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

interface OfferingSlotRepository extends JpaRepository<OfferingSlot, UUID> {

    List<OfferingSlot> findByOfferingIdOrderByDayOfWeekAscStartTimeAsc(UUID offeringId);

    boolean existsByOfferingIdAndDayOfWeekAndStartTimeAndEndTimeAndLessonType(
            UUID offeringId, int dayOfWeek, LocalTime startTime, LocalTime endTime, String lessonType);
}
