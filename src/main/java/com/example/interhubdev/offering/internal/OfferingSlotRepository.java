package com.example.interhubdev.offering.internal;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

/**
 * Repository for offering weekly slots.
 */
interface OfferingSlotRepository extends JpaRepository<OfferingSlot, UUID> {

    List<OfferingSlot> findByOfferingIdOrderByLessonTypeAscCreatedAtAsc(UUID offeringId);

    boolean existsByOfferingIdAndTimeslotIdAndLessonType(UUID offeringId, UUID timeslotId, String lessonType);

    void deleteByOfferingId(UUID offeringId);
}
