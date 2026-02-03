package com.example.interhubdev.subject.internal;

import com.example.interhubdev.subject.AssessmentTypeDto;
import com.example.interhubdev.subject.SubjectApi;
import com.example.interhubdev.subject.SubjectDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/subjects")
@RequiredArgsConstructor
@Tag(name = "Subjects", description = "Subject and assessment type catalog")
class SubjectController {

    private final SubjectApi subjectApi;

    @GetMapping
    @Operation(summary = "Get all subjects")
    public ResponseEntity<List<SubjectDto>> findAllSubjects() {
        return ResponseEntity.ok(subjectApi.findAllSubjects());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get subject by ID")
    public ResponseEntity<SubjectDto> findSubjectById(@PathVariable UUID id) {
        return subjectApi.findSubjectById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/code/{code}")
    @Operation(summary = "Get subject by code")
    public ResponseEntity<SubjectDto> findSubjectByCode(@PathVariable String code) {
        return subjectApi.findSubjectByCode(code)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('MODERATOR', 'ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Create subject", description = "Only MODERATOR, ADMIN, SUPER_ADMIN can create subjects")
    public ResponseEntity<SubjectDto> createSubject(@Valid @RequestBody CreateSubjectRequest request) {
        SubjectDto dto = subjectApi.createSubject(request.code(), request.name(), request.description());
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('MODERATOR', 'ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Update subject", description = "Only MODERATOR, ADMIN, SUPER_ADMIN can update subjects")
    public ResponseEntity<SubjectDto> updateSubject(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateSubjectRequest request
    ) {
        SubjectDto dto = subjectApi.updateSubject(id, request.name(), request.description());
        return ResponseEntity.ok(dto);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('MODERATOR', 'ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Delete subject", description = "Only MODERATOR, ADMIN, SUPER_ADMIN can delete subjects")
    public ResponseEntity<Void> deleteSubject(@PathVariable UUID id) {
        subjectApi.deleteSubject(id);
        return ResponseEntity.noContent().build();
    }

    // --- Assessment types ---
    @GetMapping("/assessment-types")
    @Operation(summary = "Get all assessment types")
    public ResponseEntity<List<AssessmentTypeDto>> findAllAssessmentTypes() {
        return ResponseEntity.ok(subjectApi.findAllAssessmentTypes());
    }

    @GetMapping("/assessment-types/{id}")
    @Operation(summary = "Get assessment type by ID")
    public ResponseEntity<AssessmentTypeDto> findAssessmentTypeById(@PathVariable UUID id) {
        return subjectApi.findAssessmentTypeById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/assessment-types")
    @PreAuthorize("hasAnyRole('MODERATOR', 'ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Create assessment type", description = "Only MODERATOR, ADMIN, SUPER_ADMIN can create assessment types")
    public ResponseEntity<AssessmentTypeDto> createAssessmentType(@Valid @RequestBody CreateAssessmentTypeRequest request) {
        AssessmentTypeDto dto = subjectApi.createAssessmentType(request.code(), request.name());
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }

    @DeleteMapping("/assessment-types/{id}")
    @PreAuthorize("hasAnyRole('MODERATOR', 'ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Delete assessment type", description = "Only MODERATOR, ADMIN, SUPER_ADMIN can delete assessment types")
    public ResponseEntity<Void> deleteAssessmentType(@PathVariable UUID id) {
        subjectApi.deleteAssessmentType(id);
        return ResponseEntity.noContent().build();
    }

    record CreateSubjectRequest(
            @NotBlank(message = "Code is required") String code,
            @NotBlank(message = "Name is required") String name,
            String description
    ) {}
    record UpdateSubjectRequest(String name, String description) {}
    record CreateAssessmentTypeRequest(
            @NotBlank(message = "Code is required") String code,
            @NotBlank(message = "Name is required") String name
    ) {}
}
