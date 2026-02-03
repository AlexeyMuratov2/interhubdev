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
        return offeringApi.findOfferingById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('MODERATOR', 'ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Create offering", description = "Only MODERATOR, ADMIN, SUPER_ADMIN can create offerings")
    public ResponseEntity<GroupSubjectOfferingDto> createOffering(@RequestBody CreateOfferingRequest request) {
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
}
