package com.example.interhubdev.schedule.internal;

import com.example.interhubdev.schedule.BuildingDto;
import com.example.interhubdev.schedule.LessonDto;
import com.example.interhubdev.schedule.LessonForScheduleDto;
import com.example.interhubdev.schedule.RoomCreateRequest;
import com.example.interhubdev.schedule.RoomDto;
import com.example.interhubdev.schedule.ScheduleApi;
import com.example.interhubdev.schedule.TimeslotCreateRequest;
import com.example.interhubdev.schedule.TimeslotDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** REST controller for schedule: buildings, rooms, timeslots, lessons. Delegates to ScheduleApi. */
@RestController
@RequestMapping("/api/schedule")
@RequiredArgsConstructor
@Tag(name = "Schedule", description = "Buildings, rooms, timeslots")
class ScheduleController {

    private final ScheduleApi scheduleApi;

    @GetMapping("/buildings")
    @Operation(summary = "Get all buildings")
    public ResponseEntity<List<BuildingDto>> findAllBuildings() {
        return ResponseEntity.ok(scheduleApi.findAllBuildings());
    }

    @GetMapping("/buildings/{id}")
    @Operation(summary = "Get building by ID")
    public ResponseEntity<BuildingDto> findBuildingById(@PathVariable UUID id) {
        return ResponseEntity.ok(scheduleApi.findBuildingById(id)
                .orElseThrow(() -> ScheduleErrors.buildingNotFound(id)));
    }

    @PostMapping("/buildings")
    @PreAuthorize("hasAnyRole('MODERATOR', 'ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Create building", description = "Only MODERATOR, ADMIN, SUPER_ADMIN can create buildings")
    public ResponseEntity<BuildingDto> createBuilding(@Valid @RequestBody CreateBuildingRequest request) {
        BuildingDto dto = scheduleApi.createBuilding(request.name(), request.address());
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }

    @PutMapping("/buildings/{id}")
    @PreAuthorize("hasAnyRole('MODERATOR', 'ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Update building", description = "Only MODERATOR, ADMIN, SUPER_ADMIN can update buildings")
    public ResponseEntity<BuildingDto> updateBuilding(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateBuildingRequest request
    ) {
        BuildingDto dto = scheduleApi.updateBuilding(id, request.name(), request.address());
        return ResponseEntity.ok(dto);
    }

    @DeleteMapping("/buildings/{id}")
    @PreAuthorize("hasAnyRole('MODERATOR', 'ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Delete building", description = "Only MODERATOR, ADMIN, SUPER_ADMIN can delete buildings; fails if building has rooms")
    public ResponseEntity<Void> deleteBuilding(@PathVariable UUID id) {
        scheduleApi.deleteBuilding(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/rooms")
    @Operation(summary = "Get all rooms")
    public ResponseEntity<List<RoomDto>> findAllRooms() {
        return ResponseEntity.ok(scheduleApi.findAllRooms());
    }

    @GetMapping("/rooms/{id}")
    @Operation(summary = "Get room by ID")
    public ResponseEntity<RoomDto> findRoomById(@PathVariable UUID id) {
        return ResponseEntity.ok(scheduleApi.findRoomById(id)
                .orElseThrow(() -> ScheduleErrors.roomNotFound(id)));
    }

    @PostMapping("/rooms")
    @PreAuthorize("hasAnyRole('MODERATOR', 'ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Create room", description = "Only MODERATOR, ADMIN, SUPER_ADMIN can create rooms")
    public ResponseEntity<RoomDto> createRoom(@Valid @RequestBody RoomCreateRequest request) {
        RoomDto dto = scheduleApi.createRoom(
                request.buildingId(),
                request.number(),
                request.capacity(),
                request.type()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }

    @PostMapping("/rooms/bulk")
    @PreAuthorize("hasAnyRole('MODERATOR', 'ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Create rooms in bulk", description = "Only MODERATOR, ADMIN, SUPER_ADMIN; all-or-nothing transaction")
    public ResponseEntity<List<RoomDto>> createRoomsBulk(@Valid @RequestBody List<RoomCreateRequest> request) {
        List<RoomDto> dtos = scheduleApi.createRoomsInBulk(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(dtos);
    }

    @PutMapping("/rooms/{id}")
    @PreAuthorize("hasAnyRole('MODERATOR', 'ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Update room", description = "Only MODERATOR, ADMIN, SUPER_ADMIN can update rooms")
    public ResponseEntity<RoomDto> updateRoom(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateRoomRequest request
    ) {
        RoomDto dto = scheduleApi.updateRoom(
                id,
                request.buildingId(),
                request.number(),
                request.capacity(),
                request.type()
        );
        return ResponseEntity.ok(dto);
    }

    @DeleteMapping("/rooms/{id}")
    @PreAuthorize("hasAnyRole('MODERATOR', 'ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Delete room", description = "Only MODERATOR, ADMIN, SUPER_ADMIN can delete rooms")
    public ResponseEntity<Void> deleteRoom(@PathVariable UUID id) {
        scheduleApi.deleteRoom(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/timeslots")
    @Operation(summary = "Get all timeslots (time templates for UI)")
    public ResponseEntity<List<TimeslotDto>> findAllTimeslots() {
        return ResponseEntity.ok(scheduleApi.findAllTimeslots());
    }

    @GetMapping("/timeslots/{id}")
    @Operation(summary = "Get timeslot by ID")
    public ResponseEntity<TimeslotDto> findTimeslotById(@PathVariable UUID id) {
        return ResponseEntity.ok(scheduleApi.findTimeslotById(id)
                .orElseThrow(() -> ScheduleErrors.timeslotNotFound(id)));
    }

    @PostMapping("/timeslots")
    @PreAuthorize("hasAnyRole('MODERATOR', 'ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Create timeslot", description = "Only MODERATOR, ADMIN, SUPER_ADMIN can create timeslots")
    public ResponseEntity<TimeslotDto> createTimeslot(@Valid @RequestBody TimeslotCreateRequest request) {
        TimeslotDto dto = scheduleApi.createTimeslot(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }

    @PostMapping("/timeslots/bulk")
    @PreAuthorize("hasAnyRole('MODERATOR', 'ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Create timeslots in bulk", description = "Only MODERATOR, ADMIN, SUPER_ADMIN; all-or-nothing transaction")
    public ResponseEntity<List<TimeslotDto>> createTimeslotsBulk(@Valid @RequestBody List<TimeslotCreateRequest> request) {
        List<TimeslotDto> dtos = scheduleApi.createTimeslotsInBulk(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(dtos);
    }

    @DeleteMapping("/timeslots/{id}")
    @PreAuthorize("hasAnyRole('MODERATOR', 'ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Delete timeslot", description = "Only MODERATOR, ADMIN, SUPER_ADMIN can delete timeslots; lessons keep data, timeslotId set to null")
    public ResponseEntity<Void> deleteTimeslot(@PathVariable UUID id) {
        scheduleApi.deleteTimeslot(id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/timeslots")
    @PreAuthorize("hasAnyRole('MODERATOR', 'ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Delete all timeslots", description = "Deletes all timeslots; lessons that referenced a slot are kept with timeslotId set to null")
    public ResponseEntity<Void> deleteAllTimeslots() {
        scheduleApi.deleteAllTimeslots();
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/lessons")
    @Operation(summary = "Get lessons by date with offering, slot and teachers for schedule UI")
    public ResponseEntity<List<LessonForScheduleDto>> findLessonsByDate(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(scheduleApi.findLessonsByDate(date));
    }

    @GetMapping("/lessons/week")
    @Operation(summary = "Get lessons for the week containing the date (Mon–Sun) with full context", description = "Returns all lessons in the ISO week (Monday to Sunday) that contains the given date. Same response structure as GET /lessons?date= (LessonForScheduleDto). Ordered by date, then startTime. Batch-loaded, no N+1.")
    public ResponseEntity<List<LessonForScheduleDto>> findLessonsByWeek(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(scheduleApi.findLessonsByWeek(date));
    }

    @GetMapping("/lessons/week/group/{groupId}")
    @Operation(summary = "Get lessons for the week for a group with full context", description = "Returns lessons in the ISO week that contains the given date, filtered by group (offerings of the group). Same response structure as GET /lessons/week. 404 if group does not exist; empty list if group has no offerings or no lessons in the week. Batch-loaded, no N+1.")
    public ResponseEntity<List<LessonForScheduleDto>> findLessonsByWeekAndGroupId(
            @PathVariable UUID groupId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(scheduleApi.findLessonsByWeekAndGroupId(date, groupId));
    }

    @GetMapping("/lessons/offering/{offeringId}")
    @Operation(summary = "Get lessons by offering ID")
    public ResponseEntity<List<LessonDto>> findLessonsByOfferingId(@PathVariable UUID offeringId) {
        return ResponseEntity.ok(scheduleApi.findLessonsByOfferingId(offeringId));
    }

    @GetMapping("/lessons/group/{groupId}")
    @Operation(summary = "Get lessons by date for a group with offering, slot and teachers", description = "Returns lessons on the given date for all offerings of the group with full context for schedule UI.")
    public ResponseEntity<List<LessonForScheduleDto>> findLessonsByDateAndGroupId(
            @PathVariable UUID groupId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(scheduleApi.findLessonsByDateAndGroupId(date, groupId));
    }

    @GetMapping("/lessons/{id}")
    @Operation(summary = "Get lesson by ID")
    public ResponseEntity<LessonDto> findLessonById(@PathVariable UUID id) {
        return ResponseEntity.ok(scheduleApi.findLessonById(id)
                .orElseThrow(() -> ScheduleErrors.lessonNotFound(id)));
    }

    @PostMapping("/lessons")
    @PreAuthorize("hasAnyRole('MODERATOR', 'ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Create lesson", description = "Lesson owns time (startTime, endTime). timeslotId optional (UI hint).")
    public ResponseEntity<LessonDto> createLesson(@Valid @RequestBody CreateLessonRequest request) {
        LessonDto dto = scheduleApi.createLesson(
                request.offeringId(),
                request.date(),
                request.startTime(),
                request.endTime(),
                request.timeslotId(),
                request.roomId(),
                request.topic(),
                request.status()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }

    @PutMapping("/lessons/{id}")
    @PreAuthorize("hasAnyRole('MODERATOR', 'ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Update lesson", description = "Can update time (startTime, endTime), room, topic, status")
    public ResponseEntity<LessonDto> updateLesson(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateLessonRequest request
    ) {
        LessonDto dto = scheduleApi.updateLesson(id, request.startTime(), request.endTime(), request.roomId(), request.topic(), request.status());
        return ResponseEntity.ok(dto);
    }

    @DeleteMapping("/lessons/{id}")
    @PreAuthorize("hasAnyRole('MODERATOR', 'ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Delete lesson", description = "Only MODERATOR, ADMIN, SUPER_ADMIN can delete lessons")
    public ResponseEntity<Void> deleteLesson(@PathVariable UUID id) {
        scheduleApi.deleteLesson(id);
        return ResponseEntity.noContent().build();
    }

    record CreateBuildingRequest(
            @NotBlank(message = "Name is required") String name,
            String address
    ) {}
    record UpdateBuildingRequest(String name, String address) {}
    record UpdateRoomRequest(UUID buildingId, String number,
                             @Min(value = 0, message = "Capacity must be >= 0") Integer capacity,
                             String type) {}
    record CreateLessonRequest(
            @NotNull(message = "Offering id is required") UUID offeringId,
            @NotBlank(message = "Date is required") String date,
            @NotBlank(message = "Start time is required") String startTime,
            @NotBlank(message = "End time is required") String endTime,
            UUID timeslotId,
            UUID roomId,
            String topic,
            String status
    ) {}
    record UpdateLessonRequest(String startTime, String endTime, UUID roomId, String topic, String status) {}
}
