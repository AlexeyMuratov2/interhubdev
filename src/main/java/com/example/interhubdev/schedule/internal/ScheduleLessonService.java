package com.example.interhubdev.schedule.internal;

import com.example.interhubdev.error.Errors;
import com.example.interhubdev.schedule.LessonBulkCreateRequest;
import com.example.interhubdev.schedule.LessonDto;
import com.example.interhubdev.schedule.OfferingLookupPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

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
        return lessonRepository.findByOfferingIdOrderByDateAscTimeslotIdAsc(offeringId).stream()
                .map(ScheduleMappers::toLessonDto)
                .toList();
    }

    List<LessonDto> findByDate(LocalDate date) {
        return lessonRepository.findByDateOrderByTimeslotIdAsc(date).stream()
                .map(ScheduleMappers::toLessonDto)
                .toList();
    }

    @Transactional
    LessonDto create(UUID offeringId, LocalDate date, UUID timeslotId, UUID roomId, String topic, String status) {
        if (offeringId == null) {
            throw Errors.badRequest("Offering id is required");
        }
        if (date == null) {
            throw Errors.badRequest("Date is required");
        }
        if (timeslotId == null) {
            throw Errors.badRequest("Timeslot id is required");
        }
        if (!offeringLookupPort.existsById(offeringId)) {
            throw Errors.notFound("Offering not found: " + offeringId);
        }
        if (timeslotRepository.findById(timeslotId).isEmpty()) {
            throw Errors.notFound("Timeslot not found: " + timeslotId);
        }
        if (roomId != null && roomRepository.findById(roomId).isEmpty()) {
            throw Errors.notFound("Room not found: " + roomId);
        }
        if (lessonRepository.existsByOfferingIdAndDateAndTimeslotId(offeringId, date, timeslotId)) {
            throw Errors.conflict("Lesson already exists for this offering, date and timeslot");
        }
        String normalizedStatus = ScheduleValidation.normalizeLessonStatus(status);
        Lesson entity = Lesson.builder()
                .offeringId(offeringId)
                .date(date)
                .timeslotId(timeslotId)
                .roomId(roomId)
                .topic(topic != null ? topic.trim() : null)
                .status(normalizedStatus)
                .build();
        return ScheduleMappers.toLessonDto(lessonRepository.save(entity));
    }

    @Transactional
    LessonDto update(UUID id, UUID roomId, String topic, String status) {
        Lesson entity = lessonRepository.findById(id)
                .orElseThrow(() -> Errors.notFound("Lesson not found: " + id));
        if (roomId != null && roomRepository.findById(roomId).isEmpty()) {
            throw Errors.notFound("Room not found: " + roomId);
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
            throw Errors.notFound("Lesson not found: " + id);
        }
        lessonRepository.deleteById(id);
    }

    /**
     * Create multiple lessons in a batch. Skips duplicates (same offering+date+timeslot).
     * Caller is responsible for ensuring offering/timeslot existence.
     */
    @Transactional
    List<LessonDto> createBulk(List<LessonBulkCreateRequest> requests) {
        List<Lesson> entities = new ArrayList<>();
        for (LessonBulkCreateRequest req : requests) {
            if (lessonRepository.existsByOfferingIdAndDateAndTimeslotId(
                    req.offeringId(), req.date(), req.timeslotId())) {
                continue; // skip duplicates silently
            }
            String normalizedStatus = ScheduleValidation.normalizeLessonStatus(req.status());
            entities.add(Lesson.builder()
                    .offeringId(req.offeringId())
                    .date(req.date())
                    .timeslotId(req.timeslotId())
                    .roomId(req.roomId())
                    .status(normalizedStatus)
                    .build());
        }
        return lessonRepository.saveAll(entities).stream()
                .map(ScheduleMappers::toLessonDto)
                .toList();
    }

    /**
     * Delete all lessons for a given offering.
     */
    @Transactional
    void deleteByOfferingId(UUID offeringId) {
        lessonRepository.deleteByOfferingId(offeringId);
    }
}
