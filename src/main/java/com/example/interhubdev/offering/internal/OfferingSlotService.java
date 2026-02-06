package com.example.interhubdev.offering.internal;

import com.example.interhubdev.error.Errors;
import com.example.interhubdev.offering.OfferingSlotDto;
import com.example.interhubdev.offering.TimeslotLookupPort;
import com.example.interhubdev.teacher.TeacherApi;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Internal service for offering slot CRUD: weekly timeslot assignments for offerings.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
class OfferingSlotService {

    private final OfferingSlotRepository slotRepository;
    private final GroupSubjectOfferingRepository offeringRepository;
    private final TimeslotLookupPort timeslotLookupPort;
    private final TeacherApi teacherApi;

    List<OfferingSlotDto> findByOfferingId(UUID offeringId) {
        return slotRepository.findByOfferingIdOrderByLessonTypeAscCreatedAtAsc(offeringId).stream()
                .map(OfferingMappers::toSlotDto)
                .toList();
    }

    @Transactional
    OfferingSlotDto add(UUID offeringId, UUID timeslotId, String lessonType, UUID roomId, UUID teacherId) {
        if (offeringId == null) {
            throw Errors.badRequest("Offering id is required");
        }
        if (timeslotId == null) {
            throw Errors.badRequest("Timeslot id is required");
        }
        if (!offeringRepository.existsById(offeringId)) {
            throw Errors.notFound("Offering not found: " + offeringId);
        }
        if (!timeslotLookupPort.existsById(timeslotId)) {
            throw Errors.notFound("Timeslot not found: " + timeslotId);
        }
        if (teacherId != null && teacherApi.findById(teacherId).isEmpty()) {
            throw Errors.notFound("Teacher not found: " + teacherId);
        }
        String normalizedType = OfferingValidation.normalizeLessonType(lessonType);
        if (slotRepository.existsByOfferingIdAndTimeslotIdAndLessonType(offeringId, timeslotId, normalizedType)) {
            throw Errors.conflict("Offering slot already exists for this timeslot and lesson type");
        }
        OfferingSlot entity = OfferingSlot.builder()
                .offeringId(offeringId)
                .timeslotId(timeslotId)
                .lessonType(normalizedType)
                .roomId(roomId)
                .teacherId(teacherId)
                .build();
        return OfferingMappers.toSlotDto(slotRepository.save(entity));
    }

    @Transactional
    void remove(UUID id) {
        if (!slotRepository.existsById(id)) {
            throw Errors.notFound("Offering slot not found: " + id);
        }
        slotRepository.deleteById(id);
    }
}
