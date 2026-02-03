package com.example.interhubdev.schedule.internal;

import com.example.interhubdev.error.Errors;
import com.example.interhubdev.schedule.LessonDto;
import com.example.interhubdev.schedule.RoomDto;
import com.example.interhubdev.schedule.ScheduleApi;
import com.example.interhubdev.schedule.TimeslotDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
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
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
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
    @PreAuthorize("hasAnyRole('MODERATOR', 'ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Create room", description = "Only MODERATOR, ADMIN, SUPER_ADMIN can create rooms")
    public ResponseEntity<RoomDto> createRoom(@Valid @RequestBody CreateRoomRequest request) {
        RoomDto dto = scheduleApi.createRoom(
                request.building(),
                request.number(),
                request.capacity(),
                request.type()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
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
                request.building(),
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
    @PreAuthorize("hasAnyRole('MODERATOR', 'ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Create timeslot", description = "Only MODERATOR, ADMIN, SUPER_ADMIN can create timeslots")
    public ResponseEntity<TimeslotDto> createTimeslot(@Valid @RequestBody CreateTimeslotRequest request) {
        LocalTime startTime = parseTime(request.startTime(), "startTime");
        LocalTime endTime = parseTime(request.endTime(), "endTime");
        TimeslotDto dto = scheduleApi.createTimeslot(request.dayOfWeek(), startTime, endTime);
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }

    @DeleteMapping("/timeslots/{id}")
    @PreAuthorize("hasAnyRole('MODERATOR', 'ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Delete timeslot", description = "Only MODERATOR, ADMIN, SUPER_ADMIN can delete timeslots")
    public ResponseEntity<Void> deleteTimeslot(@PathVariable UUID id) {
        scheduleApi.deleteTimeslot(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/lessons")
    @Operation(summary = "Get lessons by date")
    public ResponseEntity<List<LessonDto>> findLessonsByDate(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(scheduleApi.findLessonsByDate(date));
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
    @PreAuthorize("hasAnyRole('MODERATOR', 'ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Create lesson", description = "Only MODERATOR, ADMIN, SUPER_ADMIN can create lessons")
    public ResponseEntity<LessonDto> createLesson(@Valid @RequestBody CreateLessonRequest request) {
        if (request.date() == null || request.date().isBlank()) {
            throw Errors.badRequest("Date is required");
        }
        LocalDate date;
        try {
            date = LocalDate.parse(request.date());
        } catch (DateTimeParseException e) {
            throw Errors.badRequest("Invalid date format, use ISO-8601 (yyyy-MM-dd)");
        }
        LessonDto dto = scheduleApi.createLesson(
                request.offeringId(),
                date,
                request.timeslotId(),
                request.roomId(),
                request.topic(),
                request.status()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }

    @PutMapping("/lessons/{id}")
    @PreAuthorize("hasAnyRole('MODERATOR', 'ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Update lesson", description = "Only MODERATOR, ADMIN, SUPER_ADMIN can update lessons")
    public ResponseEntity<LessonDto> updateLesson(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateLessonRequest request
    ) {
        LessonDto dto = scheduleApi.updateLesson(id, request.roomId(), request.topic(), request.status());
        return ResponseEntity.ok(dto);
    }

    @DeleteMapping("/lessons/{id}")
    @PreAuthorize("hasAnyRole('MODERATOR', 'ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Delete lesson", description = "Only MODERATOR, ADMIN, SUPER_ADMIN can delete lessons")
    public ResponseEntity<Void> deleteLesson(@PathVariable UUID id) {
        scheduleApi.deleteLesson(id);
        return ResponseEntity.noContent().build();
    }

    private static LocalTime parseTime(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw Errors.badRequest(fieldName + " is required");
        }
        try {
            return LocalTime.parse(value);
        } catch (DateTimeParseException e) {
            throw Errors.badRequest("Invalid " + fieldName + " format, use HH:mm or HH:mm:ss");
        }
    }

    record CreateRoomRequest(
            @NotBlank(message = "Building is required") String building,
            @NotBlank(message = "Number is required") String number,
            @Min(value = 0, message = "Capacity must be >= 0") Integer capacity,
            String type
    ) {}
    record UpdateRoomRequest(String building, String number,
                             @Min(value = 0, message = "Capacity must be >= 0") Integer capacity,
                             String type) {}
    record CreateTimeslotRequest(
            @Min(value = 1, message = "dayOfWeek must be 1..7") @Max(value = 7, message = "dayOfWeek must be 1..7") int dayOfWeek,
            @NotBlank(message = "startTime is required") String startTime,
            @NotBlank(message = "endTime is required") String endTime
    ) {}
    record CreateLessonRequest(
            @NotNull(message = "Offering id is required") UUID offeringId,
            @NotBlank(message = "Date is required") String date,
            @NotNull(message = "Timeslot id is required") UUID timeslotId,
            UUID roomId,
            String topic,
            String status
    ) {}
    record UpdateLessonRequest(UUID roomId, String topic, String status) {}
}
