package com.example.interhubdev.schedule.internal;

import com.example.interhubdev.error.Errors;
import com.example.interhubdev.schedule.TimeslotDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** CRUD for timeslots (time templates for UI). */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
class ScheduleTimeslotService {

    private final TimeslotRepository timeslotRepository;
    private final LessonRepository lessonRepository;

    Optional<TimeslotDto> findById(UUID id) {
        return timeslotRepository.findById(id).map(ScheduleMappers::toTimeslotDto);
    }

    List<TimeslotDto> findAll() {
        return timeslotRepository.findAllByOrderByDayOfWeekAscStartTimeAsc().stream()
                .map(ScheduleMappers::toTimeslotDto)
                .toList();
    }

    @Transactional
    TimeslotDto create(int dayOfWeek, LocalTime startTime, LocalTime endTime) {
        if (dayOfWeek < 1 || dayOfWeek > 7) {
            throw Errors.badRequest("dayOfWeek must be 1..7");
        }
        if (startTime == null || endTime == null) {
            throw Errors.badRequest("startTime and endTime are required");
        }
        if (!endTime.isAfter(startTime)) {
            throw Errors.badRequest("endTime must be after startTime");
        }
        Timeslot entity = Timeslot.builder()
                .dayOfWeek(dayOfWeek)
                .startTime(startTime)
                .endTime(endTime)
                .build();
        return ScheduleMappers.toTimeslotDto(timeslotRepository.save(entity));
    }

    @Transactional
    List<TimeslotDto> createBulk(List<TimeslotBulkItem> items) {
        List<TimeslotDto> result = new java.util.ArrayList<>(items.size());
        for (TimeslotBulkItem item : items) {
            result.add(create(item.dayOfWeek(), item.startTime(), item.endTime()));
        }
        return result;
    }

    @Transactional
    void delete(UUID id) {
        if (!timeslotRepository.existsById(id)) {
            throw ScheduleErrors.timeslotNotFound(id);
        }
        lessonRepository.clearTimeslotReference(id);
        timeslotRepository.deleteById(id);
    }

    /** Deletes all timeslots. Lessons that referenced a timeslot keep their data but have timeslotId set to null. */
    @Transactional
    void deleteAll() {
        lessonRepository.clearAllTimeslotReferences();
        timeslotRepository.deleteAll();
    }

    record TimeslotBulkItem(int dayOfWeek, LocalTime startTime, LocalTime endTime) {}
}
