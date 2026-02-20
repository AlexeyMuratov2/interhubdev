package com.example.interhubdev.grades.internal;

import com.example.interhubdev.error.Errors;
import com.example.interhubdev.grades.*;
import com.example.interhubdev.auth.AuthApi;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * REST controller for Grades API: entries CRUD, student offering grades, group summary.
 */
@RestController
@RequestMapping("/api/grades")
@RequiredArgsConstructor
@Tag(name = "Grades", description = "Grade (points) ledger per student per offering. Teachers and admins only.")
class GradesController {

    private final GradesApi gradesApi;
    private final AuthApi authApi;

    private UUID requireCurrentUser(HttpServletRequest request) {
        return authApi.getCurrentUser(request)
                .map(u -> u.id())
                .orElseThrow(() -> Errors.unauthorized("Authentication required"));
    }

    @PostMapping("/entries")
    @Operation(summary = "Create grade entry", description = "Create a single grade allocation. Requires TEACHER or ADMIN role.")
    public ResponseEntity<GradeEntryDto> create(
            @Valid @RequestBody CreateGradeEntryRequest body,
            HttpServletRequest request
    ) {
        UUID gradedBy = requireCurrentUser(request);
        GradeEntryDto dto = gradesApi.create(
                body.studentId(),
                body.offeringId(),
                body.points(),
                body.typeCode(),
                body.typeLabel(),
                body.description(),
                body.lessonSessionId(),
                body.homeworkSubmissionId(),
                body.gradedAt(),
                gradedBy
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }

    @PostMapping("/entries/bulk")
    @Operation(summary = "Bulk create grade entries", description = "Create multiple entries for one offering. All-or-nothing. Requires TEACHER or ADMIN role.")
    public ResponseEntity<List<GradeEntryDto>> createBulk(
            @Valid @RequestBody BulkCreateGradeEntriesRequest body,
            HttpServletRequest request
    ) {
        UUID gradedBy = requireCurrentUser(request);
        List<GradeEntryDto> list = gradesApi.createBulk(
                body.offeringId(),
                body.typeCode(),
                body.typeLabel(),
                body.description(),
                body.lessonSessionId(),
                body.gradedAt(),
                body.items(),
                gradedBy
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(list);
    }

    @PutMapping("/entries/{id}")
    @Operation(summary = "Update grade entry", description = "Update points, type, description, or links. Entry must be ACTIVE. Requires TEACHER or ADMIN role.")
    public ResponseEntity<GradeEntryDto> update(
            @PathVariable UUID id,
            @RequestBody UpdateGradeEntryRequest body,
            HttpServletRequest request
    ) {
        UUID requesterId = requireCurrentUser(request);
        GradeEntryDto dto = gradesApi.update(
                id,
                body.points(),
                body.typeCode(),
                body.typeLabel(),
                body.description(),
                body.lessonSessionId(),
                body.homeworkSubmissionId(),
                body.gradedAt(),
                requesterId
        );
        return ResponseEntity.ok(dto);
    }

    @DeleteMapping("/entries/{id}")
    @Operation(summary = "Void grade entry", description = "Soft-delete: status VOIDED, excluded from totals. Requires TEACHER or ADMIN role.")
    public ResponseEntity<Void> voidEntry(
            @PathVariable UUID id,
            HttpServletRequest request
    ) {
        UUID gradedBy = requireCurrentUser(request);
        gradesApi.voidEntry(id, gradedBy);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/entries/{id}")
    @Operation(summary = "Get grade entry by id")
    public ResponseEntity<GradeEntryDto> getById(@PathVariable UUID id, HttpServletRequest request) {
        UUID requesterId = requireCurrentUser(request);
        return gradesApi.getById(id, requesterId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/students/{studentId}/offerings/{offeringId}")
    @Operation(summary = "Get student grades by offering", description = "Entries, total points, and breakdown by type. Query: from, to (ISO datetime), includeVoided (default false).")
    public ResponseEntity<StudentOfferingGradesDto> getStudentOfferingGrades(
            @PathVariable UUID studentId,
            @PathVariable UUID offeringId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @RequestParam(defaultValue = "false") boolean includeVoided,
            HttpServletRequest request
    ) {
        UUID requesterId = requireCurrentUser(request);
        StudentOfferingGradesDto dto = gradesApi.getStudentOfferingGrades(studentId, offeringId, from, to, includeVoided, requesterId);
        return ResponseEntity.ok(dto);
    }

    @GetMapping("/groups/{groupId}/offerings/{offeringId}/summary")
    @Operation(summary = "Get group offering summary", description = "Per-student total and breakdown for one offering. Query: from, to, includeVoided (default false).")
    public ResponseEntity<GroupOfferingSummaryDto> getGroupOfferingSummary(
            @PathVariable UUID groupId,
            @PathVariable UUID offeringId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @RequestParam(defaultValue = "false") boolean includeVoided,
            HttpServletRequest request
    ) {
        UUID requesterId = requireCurrentUser(request);
        GroupOfferingSummaryDto dto = gradesApi.getGroupOfferingSummary(groupId, offeringId, from, to, includeVoided, requesterId);
        return ResponseEntity.ok(dto);
    }

    @PutMapping("/lessons/{lessonId}/students/{studentId}/points")
    @Operation(summary = "Set or replace points for lesson", description = "Set points for one student for one lesson. Replaces previous points for this lesson with the new value; if none existed, creates one entry. For UX: single cell edit on lesson screen.")
    public ResponseEntity<GradeEntryDto> setPointsForLesson(
            @PathVariable UUID lessonId,
            @PathVariable UUID studentId,
            @Valid @RequestBody SetLessonPointsRequest body,
            HttpServletRequest request
    ) {
        UUID requesterId = requireCurrentUser(request);
        GradeEntryDto dto = gradesApi.setPointsForLesson(lessonId, studentId, body.points(), requesterId);
        return ResponseEntity.ok(dto);
    }
}
