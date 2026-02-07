package com.example.interhubdev.schedule.internal;

import com.example.interhubdev.error.Errors;
import com.example.interhubdev.schedule.LessonBulkCreateRequest;
import com.example.interhubdev.schedule.internal.ScheduleErrors;
import com.example.interhubdev.schedule.LessonDto;
import com.example.interhubdev.schedule.OfferingLookupPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** CRUD for lessons; validates offering via OfferingLookupPort. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
class ScheduleLessonService {

    private final LessonRepository lessonRepository;
    private final RoomRepository roomRepository;
    private final TimeslotRepository timeslotRepository;
    private final OfferingLookupPort offeringLookupPort;

    Optional<LessonDto> findById(UUID id) {
        return lessonRepository.findById(id).map(ScheduleMappers::toLessonDto);
    }

    List<LessonDto> findByOfferingId(UUID offeringId) {
        return lessonRepository.findByOfferingIdOrderByDateAscStartTimeAsc(offeringId).stream()
                .map(ScheduleMappers::toLessonDto)
                .toList();
    }

    List<LessonDto> findByDate(LocalDate date) {
        return lessonRepository.findByDateOrderByStartTimeAsc(date).stream()
                .map(ScheduleMappers::toLessonDto)
                .toList();
    }

    /**
     * Create lesson with date and times as strings (parsed inside). Delegates to create(..., LocalDate, LocalTime, LocalTime, ...).
     */
    @Transactional
    LessonDto create(UUID offeringId, String dateStr, String startTimeStr, String endTimeStr,
                     UUID timeslotId, UUID roomId, String topic, String status) {
        LocalDate date = ScheduleValidation.parseDate(dateStr, "date");
        LocalTime startTime = ScheduleValidation.parseTime(startTimeStr, "startTime");
        LocalTime endTime = ScheduleValidation.parseTime(endTimeStr, "endTime");
        return create(offeringId, null, date, startTime, endTime, timeslotId, roomId, topic, status);
    }

    @Transactional
    LessonDto create(UUID offeringId, UUID offeringSlotId, LocalDate date, LocalTime startTime, LocalTime endTime,
                     UUID timeslotId, UUID roomId, String topic, String status) {
        if (offeringId == null) {
            throw Errors.badRequest("Offering id is required");
        }
        if (date == null) {
            throw Errors.badRequest("Date is required");
        }
        if (startTime == null || endTime == null) {
            throw Errors.badRequest("Start time and end time are required");
        }
        if (!endTime.isAfter(startTime)) {
            throw Errors.badRequest("End time must be after start time");
        }
        if (!offeringLookupPort.existsById(offeringId)) {
            throw ScheduleErrors.offeringNotFound(offeringId);
        }
        if (timeslotId != null && timeslotRepository.findById(timeslotId).isEmpty()) {
            throw ScheduleErrors.timeslotNotFound(timeslotId);
        }
        if (roomId != null && roomRepository.findById(roomId).isEmpty()) {
            throw ScheduleErrors.roomNotFound(roomId);
        }
        if (lessonRepository.existsByOfferingIdAndDateAndStartTimeAndEndTime(offeringId, date, startTime, endTime)) {
            throw ScheduleErrors.lessonAlreadyExists();
        }
        String normalizedStatus = ScheduleValidation.normalizeLessonStatus(status);
        Lesson entity = Lesson.builder()
                .offeringId(offeringId)
                .offeringSlotId(offeringSlotId)
                .date(date)
                .startTime(startTime)
                .endTime(endTime)
                .timeslotId(timeslotId)
                .roomId(roomId)
                .topic(topic != null ? topic.trim() : null)
                .status(normalizedStatus)
                .build();
        return ScheduleMappers.toLessonDto(lessonRepository.save(entity));
    }

    /**
     * Update lesson with optional startTime/endTime as strings (parsed if non-blank). Delegates to update(..., LocalTime, LocalTime, ...).
     */
    @Transactional
    LessonDto update(UUID id, String startTimeStr, String endTimeStr, UUID roomId, String topic, String status) {
        LocalTime startTime = ScheduleValidation.parseTimeOptional(startTimeStr, "startTime");
        LocalTime endTime = ScheduleValidation.parseTimeOptional(endTimeStr, "endTime");
        return update(id, startTime, endTime, roomId, topic, status);
    }

    @Transactional
    LessonDto update(UUID id, LocalTime startTime, LocalTime endTime, UUID roomId, String topic, String status) {
        Lesson entity = lessonRepository.findById(id)
                .orElseThrow(() -> ScheduleErrors.lessonNotFound(id));
        if (startTime != null && endTime != null) {
            if (!endTime.isAfter(startTime)) {
                throw Errors.badRequest("End time must be after start time");
            }
            entity.setStartTime(startTime);
            entity.setEndTime(endTime);
        }
        if (roomId != null && roomRepository.findById(roomId).isEmpty()) {
            throw ScheduleErrors.roomNotFound(roomId);
        }
        if (topic != null) entity.setTopic(topic.trim());
        entity.setRoomId(roomId);
        if (status != null) {
            entity.setStatus(ScheduleValidation.normalizeLessonStatus(status));
        }
        entity.setUpdatedAt(LocalDateTime.now());
        return ScheduleMappers.toLessonDto(lessonRepository.save(entity));
    }

    @Transactional
    void delete(UUID id) {
        if (!lessonRepository.existsById(id)) {
            throw ScheduleErrors.lessonNotFound(id);
        }
        lessonRepository.deleteById(id);
    }

    @Transactional
    List<LessonDto> createBulk(List<LessonBulkCreateRequest> requests) {
        List<Lesson> entities = new ArrayList<>();
        for (LessonBulkCreateRequest req : requests) {
            if (lessonRepository.existsByOfferingIdAndDateAndStartTimeAndEndTime(
                    req.offeringId(), req.date(), req.startTime(), req.endTime())) {
                continue;
            }
            String normalizedStatus = ScheduleValidation.normalizeLessonStatus(req.status());
            entities.add(Lesson.builder()
                    .offeringId(req.offeringId())
                    .offeringSlotId(req.offeringSlotId())
                    .date(req.date())
                    .startTime(req.startTime())
                    .endTime(req.endTime())
                    .timeslotId(req.timeslotId())
                    .roomId(req.roomId())
                    .status(normalizedStatus)
                    .build());
        }
        return lessonRepository.saveAll(entities).stream()
                .map(ScheduleMappers::toLessonDto)
                .toList();
    }

    @Transactional
    void deleteByOfferingId(UUID offeringId) {
        lessonRepository.deleteByOfferingId(offeringId);
    }

    @Transactional
    void deleteByOfferingSlotId(UUID offeringSlotId) {
        lessonRepository.deleteByOfferingSlotId(offeringSlotId);
    }

    /**
     * Delete lessons for an offering that match the given weekly slot (day of week and time).
     * Used when an offering slot is removed (for legacy lessons without offeringSlotId).
     */
    @Transactional
    void deleteByOfferingIdAndDayOfWeekAndStartTimeAndEndTime(
            UUID offeringId, int dayOfWeek, LocalTime startTime, LocalTime endTime) {
        List<Lesson> lessons = lessonRepository.findByOfferingIdOrderByDateAscStartTimeAsc(offeringId).stream()
                .filter(l -> l.getDate().getDayOfWeek().getValue() == dayOfWeek
                        && l.getStartTime().equals(startTime)
                        && l.getEndTime().equals(endTime))
                .toList();
        lessonRepository.deleteAll(lessons);
    }
}
