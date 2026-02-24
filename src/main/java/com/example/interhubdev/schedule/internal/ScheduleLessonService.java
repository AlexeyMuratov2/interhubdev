package com.example.interhubdev.schedule.internal;

import com.example.interhubdev.error.Errors;
import com.example.interhubdev.schedule.LessonBulkCreateRequest;
import com.example.interhubdev.schedule.LessonDto;
import com.example.interhubdev.schedule.LessonEnrichmentData;
import com.example.interhubdev.schedule.LessonEnrichmentPort;
import com.example.interhubdev.schedule.LessonEnrichmentRequest;
import com.example.interhubdev.schedule.LessonForScheduleDto;
import com.example.interhubdev.schedule.GroupLookupPort;
import com.example.interhubdev.schedule.GroupSummaryDto;
import com.example.interhubdev.schedule.OfferingLookupPort;
import com.example.interhubdev.schedule.RoomSummaryDto;
import com.example.interhubdev.schedule.TeacherLookupPort;
import com.example.interhubdev.schedule.TeacherSummaryDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/** CRUD for lessons; validates offering via OfferingLookupPort. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
class ScheduleLessonService {

    private final LessonRepository lessonRepository;
    private final RoomRepository roomRepository;
    private final TimeslotRepository timeslotRepository;
    private final GroupLookupPort groupLookupPort;
    private final OfferingLookupPort offeringLookupPort;
    private final LessonEnrichmentPort lessonEnrichmentPort;
    private final ScheduleRoomService scheduleRoomService;
    private final TeacherLookupPort teacherLookupPort;

    Optional<LessonDto> findById(UUID id) {
        return lessonRepository.findById(id).map(ScheduleMappers::toLessonDto);
    }

    List<LessonDto> findByOfferingId(UUID offeringId) {
        return lessonRepository.findByOfferingIdOrderByDateAscStartTimeAsc(offeringId).stream()
                .map(ScheduleMappers::toLessonDto)
                .toList();
    }

    Set<UUID> findOfferingSlotIdsWithAtLeastOneLesson(Collection<UUID> offeringSlotIds) {
        if (offeringSlotIds == null || offeringSlotIds.isEmpty()) {
            return Collections.emptySet();
        }
        return lessonRepository.findDistinctOfferingSlotIdsByOfferingSlotIdIn(offeringSlotIds);
    }

    List<LessonDto> findByDate(LocalDate date) {
        return lessonRepository.findByDateOrderByStartTimeAsc(date).stream()
                .map(ScheduleMappers::toLessonDto)
                .toList();
    }

    /**
     * Lessons on the given date with enrichment (offering, slot, teachers). Batch load, no N+1.
     */
    List<LessonForScheduleDto> findByDateEnriched(LocalDate date) {
        List<LessonDto> lessons = findByDate(date);
        return enrichLessons(lessons);
    }

    /**
     * Lessons in the week containing the given date (ISO week: Monday–Sunday) with full enrichment.
     * Single query for lessons in [weekStart, weekEnd], then batch enrichment — no N+1.
     */
    List<LessonForScheduleDto> findByWeekEnriched(LocalDate date) {
        LocalDate weekStart = date.with(DayOfWeek.MONDAY);
        LocalDate weekEnd = weekStart.plusDays(6);
        List<LessonDto> lessons = lessonRepository
                .findByDateBetweenOrderByDateAscStartTimeAsc(weekStart, weekEnd).stream()
                .map(ScheduleMappers::toLessonDto)
                .toList();
        return enrichLessons(lessons);
    }

    /**
     * Lessons in the week containing the given date for the given group, with full enrichment.
     * Throws GROUP_NOT_FOUND (404) if the group does not exist; returns empty list if group has no offerings or no lessons in the week.
     * Single query for lessons in [weekStart, weekEnd] and offeringId in group's offerings, then batch enrichment — no N+1.
     */
    List<LessonForScheduleDto> findByWeekAndGroupIdEnriched(LocalDate date, UUID groupId) {
        if (!groupLookupPort.existsById(groupId)) {
            throw ScheduleErrors.groupNotFound(groupId);
        }
        List<UUID> offeringIds = offeringLookupPort.findOfferingIdsByGroupId(groupId);
        if (offeringIds.isEmpty()) {
            return List.of();
        }
        LocalDate weekStart = date.with(DayOfWeek.MONDAY);
        LocalDate weekEnd = weekStart.plusDays(6);
        List<LessonDto> lessons = lessonRepository
                .findByDateBetweenAndOfferingIdInOrderByDateAscStartTimeAsc(weekStart, weekEnd, offeringIds).stream()
                .map(ScheduleMappers::toLessonDto)
                .toList();
        return enrichLessons(lessons);
    }

    /**
     * Lessons on the given date for the given group with enrichment (offering, slot, teachers). Batch load, no N+1.
     * Throws GROUP_NOT_FOUND (404) if the group does not exist; returns empty list if group exists but has no lessons.
     */
    List<LessonForScheduleDto> findByDateAndGroupIdEnriched(LocalDate date, UUID groupId) {
        if (!groupLookupPort.existsById(groupId)) {
            throw ScheduleErrors.groupNotFound(groupId);
        }
        List<UUID> offeringIds = offeringLookupPort.findOfferingIdsByGroupId(groupId);
        if (offeringIds.isEmpty()) {
            return List.of();
        }
        List<LessonDto> lessons = lessonRepository.findByDateAndOfferingIdInOrderByStartTimeAsc(date, offeringIds).stream()
                .map(ScheduleMappers::toLessonDto)
                .toList();
        return enrichLessons(lessons);
    }

    /**
     * Lessons on the given date for the given group (all offerings of the group). Ordered by startTime.
     */
    List<LessonDto> findByDateAndGroupId(LocalDate date, UUID groupId) {
        List<UUID> offeringIds = offeringLookupPort.findOfferingIdsByGroupId(groupId);
        if (offeringIds.isEmpty()) {
            return List.of();
        }
        return lessonRepository.findByDateAndOfferingIdInOrderByStartTimeAsc(date, offeringIds).stream()
                .map(ScheduleMappers::toLessonDto)
                .toList();
    }

    /**
     * Lessons in the week containing the given date for the given teacher, with full enrichment.
     * Returns only lessons that this teacher actually conducts: if the slot has a teacher (e.g. lecturer for LECTURE),
     * the lesson is shown only to that slot teacher; otherwise to the offering's main teacher.
     * Returns empty list if teacher has no offerings or no lessons in the week.
     * Single query for lessons in [weekStart, weekEnd] and offeringId in teacher's offerings, then batch enrichment and filter — no N+1.
     */
    List<LessonForScheduleDto> findByWeekAndTeacherIdEnriched(LocalDate date, UUID teacherId) {
        List<UUID> offeringIds = offeringLookupPort.findOfferingIdsByTeacherId(teacherId);
        if (offeringIds.isEmpty()) {
            return List.of();
        }
        LocalDate weekStart = date.with(DayOfWeek.MONDAY);
        LocalDate weekEnd = weekStart.plusDays(6);
        List<LessonDto> lessons = lessonRepository
                .findByDateBetweenAndOfferingIdInOrderByDateAscStartTimeAsc(weekStart, weekEnd, offeringIds).stream()
                .map(ScheduleMappers::toLessonDto)
                .toList();
        List<LessonForScheduleDto> enriched = enrichLessons(lessons);
        return enriched.stream()
                .filter(dto -> dto.mainTeacher() != null && dto.mainTeacher().id().equals(teacherId))
                .toList();
    }

    private List<LessonForScheduleDto> enrichLessons(List<LessonDto> lessons) {
        if (lessons.isEmpty()) {
            return List.of();
        }
        List<LessonEnrichmentRequest> requests = lessons.stream()
                .map(l -> new LessonEnrichmentRequest(l.offeringId(), l.offeringSlotId()))
                .toList();
        List<LessonEnrichmentData> enrichment = lessonEnrichmentPort.getEnrichment(requests);
        if (enrichment.size() != lessons.size()) {
            throw new IllegalStateException("Enrichment size " + enrichment.size() + " != lessons size " + lessons.size());
        }
        Set<UUID> roomIds = new java.util.HashSet<>();
        List<UUID> teacherIds = new ArrayList<>();
        Set<UUID> groupIds = new java.util.HashSet<>();
        for (int i = 0; i < lessons.size(); i++) {
            LessonDto l = lessons.get(i);
            LessonEnrichmentData e = enrichment.get(i);
            if (l.roomId() != null) roomIds.add(l.roomId());
            if (e.slot() != null && e.slot().roomId() != null) roomIds.add(e.slot().roomId());
            if (e.slot() != null && e.slot().teacherId() != null) teacherIds.add(e.slot().teacherId());
            if (e.offering() != null && e.offering().teacherId() != null) teacherIds.add(e.offering().teacherId());
            e.teachers().stream().map(t -> t.teacherId()).filter(id -> id != null).forEach(teacherIds::add);
            if (e.offering() != null && e.offering().groupId() != null) {
                groupIds.add(e.offering().groupId());
            }
        }
        Map<UUID, RoomSummaryDto> roomMap = scheduleRoomService.findByIdIn(roomIds).stream()
                .collect(Collectors.toMap(RoomSummaryDto::id, r -> r));
        Map<UUID, TeacherSummaryDto> teacherMap = teacherLookupPort.getTeacherSummaries(teacherIds.stream().distinct().toList());
        Map<UUID, GroupSummaryDto> groupMap = groupLookupPort.getGroupSummaries(groupIds.stream().distinct().toList());

        List<LessonForScheduleDto> result = new ArrayList<>(lessons.size());
        for (int i = 0; i < lessons.size(); i++) {
            LessonDto l = lessons.get(i);
            LessonEnrichmentData e = enrichment.get(i);
            RoomSummaryDto room = l.roomId() != null ? roomMap.get(l.roomId())
                    : (e.slot() != null && e.slot().roomId() != null ? roomMap.get(e.slot().roomId()) : null);
            UUID mainTeacherId = e.slot() != null && e.slot().teacherId() != null ? e.slot().teacherId()
                    : (e.offering() != null && e.offering().teacherId() != null ? e.offering().teacherId() : null);
            TeacherSummaryDto mainTeacher = mainTeacherId != null ? teacherMap.get(mainTeacherId) : null;
            GroupSummaryDto group = e.offering() != null && e.offering().groupId() != null
                    ? groupMap.get(e.offering().groupId()) : null;
            result.add(new LessonForScheduleDto(
                    l,
                    e.offering(),
                    e.slot(),
                    e.teachers(),
                    room,
                    mainTeacher,
                    e.subjectName(),
                    group
            ));
        }
        return result;
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
        String statusForStorage = ScheduleValidation.normalizeLessonStatusForStorage(status);
        Lesson entity = Lesson.builder()
                .offeringId(offeringId)
                .offeringSlotId(offeringSlotId)
                .date(date)
                .startTime(startTime)
                .endTime(endTime)
                .timeslotId(timeslotId)
                .roomId(roomId)
                .topic(topic != null ? topic.trim() : null)
                .status(statusForStorage)
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
            entity.setStatus(ScheduleValidation.normalizeLessonStatusForStorage(status));
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
            String statusForStorage = ScheduleValidation.normalizeLessonStatusForStorage(req.status());
            entities.add(Lesson.builder()
                    .offeringId(req.offeringId())
                    .offeringSlotId(req.offeringSlotId())
                    .date(req.date())
                    .startTime(req.startTime())
                    .endTime(req.endTime())
                    .timeslotId(req.timeslotId())
                    .roomId(req.roomId())
                    .status(statusForStorage)
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

    /**
     * Delete lessons for an offering whose date is within the given range (inclusive).
     * Used when regenerating lessons for a single semester.
     */
    @Transactional
    void deleteByOfferingIdAndDateBetween(UUID offeringId, LocalDate startInclusive, LocalDate endInclusive) {
        lessonRepository.deleteByOfferingIdAndDateBetween(offeringId, startInclusive, endInclusive);
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
