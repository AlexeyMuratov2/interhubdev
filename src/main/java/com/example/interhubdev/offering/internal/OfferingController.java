package com.example.interhubdev.offering.internal;

import com.example.interhubdev.offering.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/offerings")
@RequiredArgsConstructor
@Tag(name = "Offerings", description = "Group subject offerings and offering teachers")
class OfferingController {

    private final OfferingApi offeringApi;

    @GetMapping("/group/{groupId}")
    @Operation(summary = "Get offerings by group ID")
    public ResponseEntity<List<GroupSubjectOfferingDto>> findOfferingsByGroupId(@PathVariable UUID groupId) {
        return ResponseEntity.ok(offeringApi.findOfferingsByGroupId(groupId));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get offering by ID")
    public ResponseEntity<GroupSubjectOfferingDto> findOfferingById(@PathVariable UUID id) {
        return ResponseEntity.ok(offeringApi.findOfferingById(id)
                .orElseThrow(() -> OfferingErrors.offeringNotFound(id)));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('MODERATOR', 'ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Create offering", description = "Only MODERATOR, ADMIN, SUPER_ADMIN can create offerings")
    public ResponseEntity<GroupSubjectOfferingDto> createOffering(@Valid @RequestBody CreateOfferingRequest request) {
        GroupSubjectOfferingDto dto = offeringApi.createOffering(
                request.groupId(),
                request.curriculumSubjectId(),
                request.teacherId(),
                request.roomId(),
                request.format(),
                request.notes()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('MODERATOR', 'ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Update offering", description = "Only MODERATOR, ADMIN, SUPER_ADMIN can update offerings")
    public ResponseEntity<GroupSubjectOfferingDto> updateOffering(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateOfferingRequest request
    ) {
        GroupSubjectOfferingDto dto = offeringApi.updateOffering(
                id,
                request.teacherId(),
                request.roomId(),
                request.format(),
                request.notes()
        );
        return ResponseEntity.ok(dto);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('MODERATOR', 'ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Delete offering", description = "Only MODERATOR, ADMIN, SUPER_ADMIN can delete offerings")
    public ResponseEntity<Void> deleteOffering(@PathVariable UUID id) {
        offeringApi.deleteOffering(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{offeringId}/teachers")
    @Operation(summary = "Get teachers for offering")
    public ResponseEntity<List<OfferingTeacherDto>> findTeachersByOfferingId(@PathVariable UUID offeringId) {
        return ResponseEntity.ok(offeringApi.findTeachersByOfferingId(offeringId));
    }

    @PostMapping("/{offeringId}/teachers")
    @PreAuthorize("hasAnyRole('MODERATOR', 'ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Add teacher to offering", description = "Only MODERATOR, ADMIN, SUPER_ADMIN can add offering teachers")
    public ResponseEntity<OfferingTeacherDto> addOfferingTeacher(
            @PathVariable UUID offeringId,
            @RequestBody AddOfferingTeacherRequest request
    ) {
        OfferingTeacherDto dto = offeringApi.addOfferingTeacher(
                offeringId,
                request.teacherId(),
                request.role()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }

    @DeleteMapping("/teachers/{id}")
    @PreAuthorize("hasAnyRole('MODERATOR', 'ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Remove teacher from offering", description = "Only MODERATOR, ADMIN, SUPER_ADMIN can remove offering teachers")
    public ResponseEntity<Void> removeOfferingTeacher(@PathVariable UUID id) {
        offeringApi.removeOfferingTeacher(id);
        return ResponseEntity.noContent().build();
    }

    // --- Offering Slots ---

    @GetMapping("/{offeringId}/slots")
    @Operation(summary = "Get weekly slots for offering")
    public ResponseEntity<List<OfferingSlotDto>> findSlotsByOfferingId(@PathVariable UUID offeringId) {
        return ResponseEntity.ok(offeringApi.findSlotsByOfferingId(offeringId));
    }

    @PostMapping("/{offeringId}/slots")
    @PreAuthorize("hasAnyRole('MODERATOR', 'ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Add weekly slot to offering", description = "Slot owns time. Provide timeslotId (copy time) or dayOfWeek+startTime+endTime")
    public ResponseEntity<OfferingSlotDto> addOfferingSlot(
            @PathVariable UUID offeringId,
            @Valid @RequestBody AddOfferingSlotRequest request
    ) {
        Integer dayOfWeek = null;
        java.time.LocalTime startTime = null;
        java.time.LocalTime endTime = null;
        if (request.timeslotId() == null) {
            dayOfWeek = request.dayOfWeek();
            startTime = OfferingValidation.parseTime(request.startTime(), "startTime");
            endTime = OfferingValidation.parseTime(request.endTime(), "endTime");
        }
        OfferingSlotDto dto = offeringApi.addOfferingSlot(
                offeringId,
                request.timeslotId(),
                dayOfWeek,
                startTime,
                endTime,
                request.lessonType(),
                request.roomId(),
                request.teacherId()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }

    @DeleteMapping("/slots/{id}")
    @PreAuthorize("hasAnyRole('MODERATOR', 'ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Remove weekly slot from offering", description = "Only MODERATOR, ADMIN, SUPER_ADMIN can remove offering slots")
    public ResponseEntity<Void> removeOfferingSlot(@PathVariable UUID id) {
        offeringApi.removeOfferingSlot(id);
        return ResponseEntity.noContent().build();
    }

    // --- Lesson Generation ---

    @PostMapping("/{offeringId}/generate-lessons")
    @PreAuthorize("hasAnyRole('MODERATOR', 'ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Generate lessons for a single offering",
            description = "Creates lessons for the given semester only: dates are within semester start/end. Requires semesterId.")
    public ResponseEntity<LessonGenerationResponse> generateLessonsForOffering(
            @PathVariable UUID offeringId,
            @RequestParam(name = "semesterId", required = true) UUID semesterId
    ) {
        int count = offeringApi.generateLessonsForOffering(offeringId, semesterId);
        return ResponseEntity.status(HttpStatus.CREATED).body(new LessonGenerationResponse(count));
    }

    @PostMapping("/group/{groupId}/generate-lessons")
    @PreAuthorize("hasAnyRole('MODERATOR', 'ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Generate lessons for all offerings of a group",
            description = "Creates lessons for the given semester only for all offerings of the group. Requires semesterId.")
    public ResponseEntity<LessonGenerationResponse> generateLessonsForGroup(
            @PathVariable UUID groupId,
            @RequestParam(name = "semesterId", required = true) UUID semesterId
    ) {
        int count = offeringApi.generateLessonsForGroup(groupId, semesterId);
        return ResponseEntity.status(HttpStatus.CREATED).body(new LessonGenerationResponse(count));
    }

    @PostMapping("/{offeringId}/regenerate-lessons")
    @PreAuthorize("hasAnyRole('MODERATOR', 'ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Delete existing lessons and regenerate for offering",
            description = "Deletes only lessons of the given semester (by date range) for the offering, then creates new ones for that semester.")
    public ResponseEntity<LessonGenerationResponse> regenerateLessonsForOffering(
            @PathVariable UUID offeringId,
            @RequestParam(name = "semesterId", required = true) UUID semesterId
    ) {
        int count = offeringApi.regenerateLessonsForOffering(offeringId, semesterId);
        return ResponseEntity.status(HttpStatus.CREATED).body(new LessonGenerationResponse(count));
    }

    record CreateOfferingRequest(
            @NotNull(message = "Group id is required") UUID groupId,
            @NotNull(message = "Curriculum subject id is required") UUID curriculumSubjectId,
            UUID teacherId,
            UUID roomId,
            String format,
            String notes
    ) {}
    record UpdateOfferingRequest(UUID teacherId, UUID roomId, String format, String notes) {}
    record AddOfferingTeacherRequest(
            @NotNull(message = "Teacher id is required") UUID teacherId,
            @NotBlank(message = "Role is required") String role
    ) {}
    record AddOfferingSlotRequest(
            UUID timeslotId,
            Integer dayOfWeek,
            String startTime,
            String endTime,
            @NotBlank(message = "Lesson type is required") String lessonType,
            UUID roomId,
            UUID teacherId
    ) {}
    record LessonGenerationResponse(int lessonsCreated) {}
}
