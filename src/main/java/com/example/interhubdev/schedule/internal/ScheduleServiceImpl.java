package com.example.interhubdev.schedule.internal;

import com.example.interhubdev.error.Errors;
import com.example.interhubdev.schedule.LessonDto;
import com.example.interhubdev.schedule.RoomDto;
import com.example.interhubdev.schedule.ScheduleApi;
import com.example.interhubdev.schedule.TimeslotDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
class ScheduleServiceImpl implements ScheduleApi {

    private final RoomRepository roomRepository;
    private final TimeslotRepository timeslotRepository;
    private final LessonRepository lessonRepository;

    @Override
    public Optional<RoomDto> findRoomById(UUID id) {
        return roomRepository.findById(id).map(this::toRoomDto);
    }

    @Override
    public List<RoomDto> findAllRooms() {
        return roomRepository.findAll().stream()
                .map(this::toRoomDto)
                .toList();
    }

    @Override
    @Transactional
    public RoomDto createRoom(String building, String number, Integer capacity, String type) {
        if (building == null || building.isBlank()) {
            throw Errors.badRequest("Room building is required");
        }
        if (number == null || number.isBlank()) {
            throw Errors.badRequest("Room number is required");
        }
        if (capacity != null && capacity < 0) {
            throw Errors.badRequest("Room capacity must be >= 0");
        }
        Room entity = Room.builder()
                .building(building.trim())
                .number(number.trim())
                .capacity(capacity)
                .type(type != null ? type.trim() : null)
                .build();
        return toRoomDto(roomRepository.save(entity));
    }

    @Override
    @Transactional
    public RoomDto updateRoom(UUID id, String building, String number, Integer capacity, String type) {
        Room entity = roomRepository.findById(id)
                .orElseThrow(() -> Errors.notFound("Room not found: " + id));
        if (building != null) entity.setBuilding(building.trim());
        if (number != null) entity.setNumber(number.trim());
        if (capacity != null) {
            if (capacity < 0) throw Errors.badRequest("Room capacity must be >= 0");
            entity.setCapacity(capacity);
        }
        if (type != null) entity.setType(type.trim());
        entity.setUpdatedAt(LocalDateTime.now());
        return toRoomDto(roomRepository.save(entity));
    }

    @Override
    @Transactional
    public void deleteRoom(UUID id) {
        if (!roomRepository.existsById(id)) {
            throw Errors.notFound("Room not found: " + id);
        }
        roomRepository.deleteById(id);
    }

    @Override
    public Optional<TimeslotDto> findTimeslotById(UUID id) {
        return timeslotRepository.findById(id).map(this::toTimeslotDto);
    }

    @Override
    public List<TimeslotDto> findAllTimeslots() {
        return timeslotRepository.findAll().stream()
                .map(this::toTimeslotDto)
                .toList();
    }

    @Override
    @Transactional
    public TimeslotDto createTimeslot(int dayOfWeek, java.time.LocalTime startTime, java.time.LocalTime endTime) {
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
        return toTimeslotDto(timeslotRepository.save(entity));
    }

    @Override
    @Transactional
    public void deleteTimeslot(UUID id) {
        if (!timeslotRepository.existsById(id)) {
            throw Errors.notFound("Timeslot not found: " + id);
        }
        timeslotRepository.deleteById(id);
    }

    @Override
    public Optional<LessonDto> findLessonById(UUID id) {
        return lessonRepository.findById(id).map(this::toLessonDto);
    }

    @Override
    public List<LessonDto> findLessonsByOfferingId(UUID offeringId) {
        return lessonRepository.findByOfferingId(offeringId).stream()
                .map(this::toLessonDto)
                .toList();
    }

    @Override
    public List<LessonDto> findLessonsByDate(LocalDate date) {
        return lessonRepository.findByDate(date).stream()
                .map(this::toLessonDto)
                .toList();
    }

    private static final List<String> VALID_LESSON_STATUSES = List.of("planned", "cancelled", "done");

    @Override
    @Transactional
    public LessonDto createLesson(UUID offeringId, LocalDate date, UUID timeslotId, UUID roomId, String topic, String status) {
        if (offeringId == null) {
            throw Errors.badRequest("Offering id is required");
        }
        if (date == null) {
            throw Errors.badRequest("Date is required");
        }
        if (timeslotId == null) {
            throw Errors.badRequest("Timeslot id is required");
        }
        if (timeslotRepository.findById(timeslotId).isEmpty()) {
            throw Errors.notFound("Timeslot not found: " + timeslotId);
        }
        if (roomId != null && roomRepository.findById(roomId).isEmpty()) {
            throw Errors.notFound("Room not found: " + roomId);
        }
        String s = status != null ? status.trim().toLowerCase() : "planned";
        if (!VALID_LESSON_STATUSES.contains(s)) {
            throw Errors.badRequest("Status must be planned, cancelled, or done");
        }
        Lesson entity = Lesson.builder()
                .offeringId(offeringId)
                .date(date)
                .timeslotId(timeslotId)
                .roomId(roomId)
                .topic(topic != null ? topic.trim() : null)
                .status(s)
                .build();
        return toLessonDto(lessonRepository.save(entity));
    }

    @Override
    @Transactional
    public LessonDto updateLesson(UUID id, UUID roomId, String topic, String status) {
        Lesson entity = lessonRepository.findById(id)
                .orElseThrow(() -> Errors.notFound("Lesson not found: " + id));
        if (roomId != null && roomRepository.findById(roomId).isEmpty()) {
            throw Errors.notFound("Room not found: " + roomId);
        }
        if (topic != null) entity.setTopic(topic.trim());
        entity.setRoomId(roomId);
        if (status != null) {
            String s = status.trim().toLowerCase();
            if (!VALID_LESSON_STATUSES.contains(s)) {
                throw Errors.badRequest("Status must be planned, cancelled, or done");
            }
            entity.setStatus(s);
        }
        entity.setUpdatedAt(LocalDateTime.now());
        return toLessonDto(lessonRepository.save(entity));
    }

    @Override
    @Transactional
    public void deleteLesson(UUID id) {
        if (!lessonRepository.existsById(id)) {
            throw Errors.notFound("Lesson not found: " + id);
        }
        lessonRepository.deleteById(id);
    }

    private RoomDto toRoomDto(Room e) {
        return new RoomDto(e.getId(), e.getBuilding(), e.getNumber(), e.getCapacity(), e.getType(),
                e.getCreatedAt(), e.getUpdatedAt());
    }

    private TimeslotDto toTimeslotDto(Timeslot e) {
        return new TimeslotDto(e.getId(), e.getDayOfWeek(), e.getStartTime(), e.getEndTime());
    }

    private LessonDto toLessonDto(Lesson e) {
        return new LessonDto(e.getId(), e.getOfferingId(), e.getDate(), e.getTimeslotId(), e.getRoomId(),
                e.getTopic(), e.getStatus(), e.getCreatedAt(), e.getUpdatedAt());
    }
}
