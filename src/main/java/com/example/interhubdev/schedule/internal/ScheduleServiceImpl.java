package com.example.interhubdev.schedule.internal;

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
        Room entity = Room.builder()
                .building(building != null ? building : "")
                .number(number != null ? number : "")
                .capacity(capacity)
                .type(type)
                .build();
        return toRoomDto(roomRepository.save(entity));
    }

    @Override
    @Transactional
    public RoomDto updateRoom(UUID id, String building, String number, Integer capacity, String type) {
        Room entity = roomRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Room not found: " + id));
        if (building != null) entity.setBuilding(building);
        if (number != null) entity.setNumber(number);
        if (capacity != null) entity.setCapacity(capacity);
        if (type != null) entity.setType(type);
        entity.setUpdatedAt(LocalDateTime.now());
        return toRoomDto(roomRepository.save(entity));
    }

    @Override
    @Transactional
    public void deleteRoom(UUID id) {
        if (!roomRepository.existsById(id)) {
            throw new IllegalArgumentException("Room not found: " + id);
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
            throw new IllegalArgumentException("dayOfWeek must be 1..7");
        }
        if (startTime == null || endTime == null) {
            throw new IllegalArgumentException("startTime and endTime are required");
        }
        if (!endTime.isAfter(startTime)) {
            throw new IllegalArgumentException("endTime must be after startTime");
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
            throw new IllegalArgumentException("Timeslot not found: " + id);
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

    @Override
    @Transactional
    public LessonDto createLesson(UUID offeringId, LocalDate date, UUID timeslotId, UUID roomId, String topic, String status) {
        if (date == null) {
            throw new IllegalArgumentException("date is required");
        }
        if (timeslotRepository.findById(timeslotId).isEmpty()) {
            throw new IllegalArgumentException("Timeslot not found: " + timeslotId);
        }
        if (roomId != null && roomRepository.findById(roomId).isEmpty()) {
            throw new IllegalArgumentException("Room not found: " + roomId);
        }
        String s = status != null ? status : "planned";
        if (!List.of("planned", "cancelled", "done").contains(s)) {
            throw new IllegalArgumentException("Status must be planned, cancelled, or done");
        }
        Lesson entity = Lesson.builder()
                .offeringId(offeringId)
                .date(date)
                .timeslotId(timeslotId)
                .roomId(roomId)
                .topic(topic)
                .status(s)
                .build();
        return toLessonDto(lessonRepository.save(entity));
    }

    @Override
    @Transactional
    public LessonDto updateLesson(UUID id, UUID roomId, String topic, String status) {
        Lesson entity = lessonRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Lesson not found: " + id));
        if (roomId != null && roomRepository.findById(roomId).isEmpty()) {
            throw new IllegalArgumentException("Room not found: " + roomId);
        }
        if (topic != null) entity.setTopic(topic);
        entity.setRoomId(roomId);
        if (status != null) {
            if (!List.of("planned", "cancelled", "done").contains(status)) {
                throw new IllegalArgumentException("Status must be planned, cancelled, or done");
            }
            entity.setStatus(status);
        }
        entity.setUpdatedAt(LocalDateTime.now());
        return toLessonDto(lessonRepository.save(entity));
    }

    @Override
    @Transactional
    public void deleteLesson(UUID id) {
        if (!lessonRepository.existsById(id)) {
            throw new IllegalArgumentException("Lesson not found: " + id);
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
