package com.example.interhubdev.schedule.internal;

import org.springframework.data.jpa.repository.JpaRepository;

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
}
