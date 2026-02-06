package com.example.interhubdev.schedule.internal;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

interface LessonRepository extends JpaRepository<Lesson, UUID> {

    List<Lesson> findByOfferingIdOrderByDateAscTimeslotIdAsc(UUID offeringId);

    List<Lesson> findByDateOrderByTimeslotIdAsc(LocalDate date);

    List<Lesson> findByOfferingIdAndDate(UUID offeringId, LocalDate date);

    boolean existsByOfferingIdAndDateAndTimeslotId(UUID offeringId, LocalDate date, UUID timeslotId);

    void deleteByOfferingId(UUID offeringId);
}
