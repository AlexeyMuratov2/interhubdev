package com.example.interhubdev.offering.internal;

import com.example.interhubdev.error.Errors;
import com.example.interhubdev.offering.LessonCreationPort;
import com.example.interhubdev.offering.OfferingSlotDto;
import com.example.interhubdev.offering.TimeslotLookupPort;
import com.example.interhubdev.teacher.TeacherApi;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

/**
 * Internal service for offering slot CRUD. Slot owns day and time; timeslotId optional (UI hint).
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
class OfferingSlotService {

    private final OfferingSlotRepository slotRepository;
    private final GroupSubjectOfferingRepository offeringRepository;
    private final TimeslotLookupPort timeslotLookupPort;
    private final TeacherApi teacherApi;
    private final LessonCreationPort lessonCreationPort;

    List<OfferingSlotDto> findByOfferingId(UUID offeringId) {
        return slotRepository.findByOfferingIdOrderByDayOfWeekAscStartTimeAsc(offeringId).stream()
                .map(OfferingMappers::toSlotDto)
                .toList();
    }

    List<OfferingSlotDto> findByTeacherId(UUID teacherId) {
        return slotRepository.findByTeacherId(teacherId).stream()
                .map(OfferingMappers::toSlotDto)
                .toList();
    }

    @Transactional
    OfferingSlotDto add(UUID offeringId, UUID timeslotId, Integer dayOfWeek, LocalTime startTime, LocalTime endTime,
                       String lessonType, UUID roomId, UUID teacherId) {
        if (offeringId == null) {
            throw Errors.badRequest("Offering id is required");
        }
        if (!offeringRepository.existsById(offeringId)) {
            throw Errors.notFound("Offering not found");
        }
        if (teacherId != null && teacherApi.findById(teacherId).isEmpty()) {
            throw Errors.notFound("Teacher not found");
        }
        String normalizedType = OfferingValidation.normalizeLessonType(lessonType);

        int effectiveDay;
        LocalTime effectiveStart;
        LocalTime effectiveEnd;
        UUID effectiveTimeslotId = null;
        if (timeslotId != null) {
            TimeslotLookupPort.TimeslotInfo info = timeslotLookupPort.findById(timeslotId)
                    .orElseThrow(() -> Errors.notFound("Timeslot not found"));
            effectiveDay = info.dayOfWeek();
            effectiveStart = info.startTime();
            effectiveEnd = info.endTime();
            effectiveTimeslotId = timeslotId;
        } else {
            if (dayOfWeek == null || startTime == null || endTime == null) {
                throw Errors.badRequest("Either timeslotId or (dayOfWeek, startTime, endTime) is required");
            }
            if (dayOfWeek < 1 || dayOfWeek > 7) {
                throw Errors.badRequest("dayOfWeek must be 1..7");
            }
            if (!endTime.isAfter(startTime)) {
                throw Errors.badRequest("endTime must be after startTime");
            }
            effectiveDay = dayOfWeek;
            effectiveStart = startTime;
            effectiveEnd = endTime;
        }

        if (slotRepository.existsByOfferingIdAndDayOfWeekAndStartTimeAndEndTimeAndLessonType(
                offeringId, effectiveDay, effectiveStart, effectiveEnd, normalizedType)) {
            throw Errors.conflict("Offering slot already exists for this day, time and lesson type");
        }
        OfferingSlot entity = OfferingSlot.builder()
                .offeringId(offeringId)
                .dayOfWeek(effectiveDay)
                .startTime(effectiveStart)
                .endTime(effectiveEnd)
                .timeslotId(effectiveTimeslotId)
                .lessonType(normalizedType)
                .roomId(roomId)
                .teacherId(teacherId)
                .build();
        return OfferingMappers.toSlotDto(slotRepository.save(entity));
    }

    @Transactional
    void remove(UUID id) {
        OfferingSlot slot = slotRepository.findById(id)
                .orElseThrow(() -> Errors.notFound("Offering slot not found"));
        lessonCreationPort.deleteLessonsByOfferingSlotId(slot.getId());
        lessonCreationPort.deleteLessonsByOfferingIdAndDayOfWeekAndStartTimeAndEndTime(
                slot.getOfferingId(),
                slot.getDayOfWeek(),
                slot.getStartTime(),
                slot.getEndTime());
        slotRepository.deleteById(id);
    }
}
