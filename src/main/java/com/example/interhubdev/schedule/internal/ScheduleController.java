package com.example.interhubdev.schedule.internal;

import com.example.interhubdev.schedule.LessonDto;
import com.example.interhubdev.schedule.RoomDto;
import com.example.interhubdev.schedule.ScheduleApi;
import com.example.interhubdev.schedule.TimeslotDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/schedule")
@RequiredArgsConstructor
@Tag(name = "Schedule", description = "Rooms and timeslots")
class ScheduleController {

    private final ScheduleApi scheduleApi;

    @GetMapping("/rooms")
    @Operation(summary = "Get all rooms")
    public ResponseEntity<List<RoomDto>> findAllRooms() {
        return ResponseEntity.ok(scheduleApi.findAllRooms());
    }

    @GetMapping("/rooms/{id}")
    @Operation(summary = "Get room by ID")
    public ResponseEntity<RoomDto> findRoomById(@PathVariable UUID id) {
        return scheduleApi.findRoomById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/rooms")
    @Operation(summary = "Create room")
    public ResponseEntity<RoomDto> createRoom(@RequestBody CreateRoomRequest request) {
        RoomDto dto = scheduleApi.createRoom(
                request.building(),
                request.number(),
                request.capacity(),
                request.type()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }

    @PutMapping("/rooms/{id}")
    @Operation(summary = "Update room")
    public ResponseEntity<RoomDto> updateRoom(
            @PathVariable UUID id,
            @RequestBody UpdateRoomRequest request
    ) {
        RoomDto dto = scheduleApi.updateRoom(
                id,
                request.building(),
                request.number(),
                request.capacity(),
                request.type()
        );
        return ResponseEntity.ok(dto);
    }

    @DeleteMapping("/rooms/{id}")
    @Operation(summary = "Delete room")
    public ResponseEntity<Void> deleteRoom(@PathVariable UUID id) {
        scheduleApi.deleteRoom(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/timeslots")
    @Operation(summary = "Get all timeslots")
    public ResponseEntity<List<TimeslotDto>> findAllTimeslots() {
        return ResponseEntity.ok(scheduleApi.findAllTimeslots());
    }

    @GetMapping("/timeslots/{id}")
    @Operation(summary = "Get timeslot by ID")
    public ResponseEntity<TimeslotDto> findTimeslotById(@PathVariable UUID id) {
        return scheduleApi.findTimeslotById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/timeslots")
    @Operation(summary = "Create timeslot")
    public ResponseEntity<TimeslotDto> createTimeslot(@RequestBody CreateTimeslotRequest request) {
        TimeslotDto dto = scheduleApi.createTimeslot(
                request.dayOfWeek(),
                request.startTime() != null ? LocalTime.parse(request.startTime()) : null,
                request.endTime() != null ? LocalTime.parse(request.endTime()) : null
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }

    @DeleteMapping("/timeslots/{id}")
    @Operation(summary = "Delete timeslot")
    public ResponseEntity<Void> deleteTimeslot(@PathVariable UUID id) {
        scheduleApi.deleteTimeslot(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/lessons")
    @Operation(summary = "Get lessons by date")
    public ResponseEntity<List<LessonDto>> findLessonsByDate(@RequestParam String date) {
        return ResponseEntity.ok(scheduleApi.findLessonsByDate(LocalDate.parse(date)));
    }

    @GetMapping("/lessons/offering/{offeringId}")
    @Operation(summary = "Get lessons by offering ID")
    public ResponseEntity<List<LessonDto>> findLessonsByOfferingId(@PathVariable UUID offeringId) {
        return ResponseEntity.ok(scheduleApi.findLessonsByOfferingId(offeringId));
    }

    @GetMapping("/lessons/{id}")
    @Operation(summary = "Get lesson by ID")
    public ResponseEntity<LessonDto> findLessonById(@PathVariable UUID id) {
        return scheduleApi.findLessonById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/lessons")
    @Operation(summary = "Create lesson")
    public ResponseEntity<LessonDto> createLesson(@RequestBody CreateLessonRequest request) {
        LessonDto dto = scheduleApi.createLesson(
                request.offeringId(),
                LocalDate.parse(request.date()),
                request.timeslotId(),
                request.roomId(),
                request.topic(),
                request.status()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }

    @PutMapping("/lessons/{id}")
    @Operation(summary = "Update lesson")
    public ResponseEntity<LessonDto> updateLesson(
            @PathVariable UUID id,
            @RequestBody UpdateLessonRequest request
    ) {
        LessonDto dto = scheduleApi.updateLesson(id, request.roomId(), request.topic(), request.status());
        return ResponseEntity.ok(dto);
    }

    @DeleteMapping("/lessons/{id}")
    @Operation(summary = "Delete lesson")
    public ResponseEntity<Void> deleteLesson(@PathVariable UUID id) {
        scheduleApi.deleteLesson(id);
        return ResponseEntity.noContent().build();
    }

    record CreateRoomRequest(String building, String number, Integer capacity, String type) {}
    record UpdateRoomRequest(String building, String number, Integer capacity, String type) {}
    record CreateTimeslotRequest(int dayOfWeek, String startTime, String endTime) {}
    record CreateLessonRequest(UUID offeringId, String date, UUID timeslotId, UUID roomId, String topic, String status) {}
    record UpdateLessonRequest(UUID roomId, String topic, String status) {}
}
