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

/**
 * REST controller for subject and assessment type catalog.
 * Exposes {@link SubjectApi} under /api/subjects and /api/subjects/assessment-types.
 * Write operations require MODERATOR, ADMIN or SUPER_ADMIN; read operations are allowed for all authenticated users.
 */
@RestController
@RequestMapping("/api/subjects")
@RequiredArgsConstructor
@Tag(name = "Subjects", description = "Subject and assessment type catalog")
class SubjectController {

    private final SubjectApi subjectApi;

    /**
     * Returns all subjects in stable order (by code ascending).
     * No auth role required beyond authenticated user.
     *
     * @return 200 OK with list of subject DTOs (may be empty)
     */
    @GetMapping
    @Operation(summary = "Get all subjects")
    public ResponseEntity<List<SubjectDto>> findAllSubjects() {
        return ResponseEntity.ok(subjectApi.findAllSubjects());
    }

    /**
     * Returns a subject by its unique id.
     *
     * @param id subject id (path variable)
     * @return 200 OK with subject DTO if found, 404 Not Found otherwise
     */
    @GetMapping("/{id}")
    @Operation(summary = "Get subject by ID")
    public ResponseEntity<SubjectDto> findSubjectById(@PathVariable UUID id) {
        return subjectApi.findSubjectById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Returns a subject by its unique code (path segment; no trim applied to path).
     *
     * @param code subject code (path variable)
     * @return 200 OK with subject DTO if found, 404 Not Found otherwise
     */
    @GetMapping("/code/{code}")
    @Operation(summary = "Get subject by code")
    public ResponseEntity<SubjectDto> findSubjectByCode(@PathVariable String code) {
        return subjectApi.findSubjectByCode(code)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Creates a new subject. Request body is validated (@Valid); code and chineseName are required.
     * Requires MODERATOR, ADMIN or SUPER_ADMIN. Returns 403 if insufficient role.
     *
     * @param request body: code (required), chineseName (required), englishName (optional), description (optional), departmentId (optional; validated if set)
     * @return 201 Created with created subject DTO, or 400/409/404 on validation/conflict/not-found
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('MODERATOR', 'ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Create subject", description = "Only MODERATOR, ADMIN, SUPER_ADMIN can create subjects")
    public ResponseEntity<SubjectDto> createSubject(@Valid @RequestBody CreateSubjectRequest request) {
        SubjectDto dto = subjectApi.createSubject(request.code(), request.chineseName(), request.englishName(), request.description(), request.departmentId());
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }

    /**
     * Updates an existing subject by id. Only non-null fields in request are updated.
     * Requires MODERATOR, ADMIN or SUPER_ADMIN.
     *
     * @param id      subject id (path variable)
     * @param request body: chineseName (optional), englishName (optional), description (optional), departmentId (optional; validated if set)
     * @return 200 OK with updated subject DTO, or 404 if subject or department not found
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('MODERATOR', 'ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Update subject", description = "Only MODERATOR, ADMIN, SUPER_ADMIN can update subjects")
    public ResponseEntity<SubjectDto> updateSubject(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateSubjectRequest request
    ) {
        SubjectDto dto = subjectApi.updateSubject(id, request.chineseName(), request.englishName(), request.description(), request.departmentId());
        return ResponseEntity.ok(dto);
    }

    /**
     * Deletes a subject by id. Requires MODERATOR, ADMIN or SUPER_ADMIN.
     *
     * @param id subject id (path variable)
     * @return 204 No Content on success, 404 if subject does not exist
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('MODERATOR', 'ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Delete subject", description = "Only MODERATOR, ADMIN, SUPER_ADMIN can delete subjects")
    public ResponseEntity<Void> deleteSubject(@PathVariable UUID id) {
        subjectApi.deleteSubject(id);
        return ResponseEntity.noContent().build();
    }

    // --- Assessment types ---

    /**
     * Returns all assessment types in stable order (sort order ascending, then code ascending).
     *
     * @return 200 OK with list of assessment type DTOs (may be empty)
     */
    @GetMapping("/assessment-types")
    @Operation(summary = "Get all assessment types")
    public ResponseEntity<List<AssessmentTypeDto>> findAllAssessmentTypes() {
        return ResponseEntity.ok(subjectApi.findAllAssessmentTypes());
    }

    /**
     * Returns an assessment type by its unique id.
     *
     * @param id assessment type id (path variable)
     * @return 200 OK with assessment type DTO if found, 404 Not Found otherwise
     */
    @GetMapping("/assessment-types/{id}")
    @Operation(summary = "Get assessment type by ID")
    public ResponseEntity<AssessmentTypeDto> findAssessmentTypeById(@PathVariable UUID id) {
        return subjectApi.findAssessmentTypeById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Creates a new assessment type. Request body is validated; code and chineseName are required.
     * Requires MODERATOR, ADMIN or SUPER_ADMIN.
     *
     * @param request body: code (required), chineseName (required), englishName (optional), isGraded (optional, default true), isFinal (optional, default false), sortOrder (optional, default 0)
     * @return 201 Created with created assessment type DTO, or 400/409 on validation/conflict
     */
    @PostMapping("/assessment-types")
    @PreAuthorize("hasAnyRole('MODERATOR', 'ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Create assessment type", description = "Only MODERATOR, ADMIN, SUPER_ADMIN can create assessment types")
    public ResponseEntity<AssessmentTypeDto> createAssessmentType(@Valid @RequestBody CreateAssessmentTypeRequest request) {
        AssessmentTypeDto dto = subjectApi.createAssessmentType(
                request.code(), request.chineseName(), request.englishName(), request.isGraded(), request.isFinal(), request.sortOrder());
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }

    /**
     * Updates an existing assessment type by id. Only non-null fields in request are updated.
     * Requires MODERATOR, ADMIN or SUPER_ADMIN.
     *
     * @param id      assessment type id (path variable)
     * @param request body: chineseName (optional), englishName (optional), isGraded (optional), isFinal (optional), sortOrder (optional)
     * @return 200 OK with updated assessment type DTO, or 404 if assessment type not found
     */
    @PutMapping("/assessment-types/{id}")
    @PreAuthorize("hasAnyRole('MODERATOR', 'ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Update assessment type", description = "Only MODERATOR, ADMIN, SUPER_ADMIN can update assessment types")
    public ResponseEntity<AssessmentTypeDto> updateAssessmentType(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateAssessmentTypeRequest request
    ) {
        AssessmentTypeDto dto = subjectApi.updateAssessmentType(
                id, request.chineseName(), request.englishName(), request.isGraded(), request.isFinal(), request.sortOrder());
        return ResponseEntity.ok(dto);
    }

    /**
     * Deletes an assessment type by id. Requires MODERATOR, ADMIN or SUPER_ADMIN.
     *
     * @param id assessment type id (path variable)
     * @return 204 No Content on success, 404 if assessment type does not exist
     */
    @DeleteMapping("/assessment-types/{id}")
    @PreAuthorize("hasAnyRole('MODERATOR', 'ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Delete assessment type", description = "Only MODERATOR, ADMIN, SUPER_ADMIN can delete assessment types")
    public ResponseEntity<Void> deleteAssessmentType(@PathVariable UUID id) {
        subjectApi.deleteAssessmentType(id);
        return ResponseEntity.noContent().build();
    }

    /** Request body for creating a subject: code and chineseName required; englishName, description, departmentId optional. */
    record CreateSubjectRequest(
            @NotBlank(message = "Code is required") String code,
            @NotBlank(message = "Chinese name is required") String chineseName,
            String englishName,
            String description,
            UUID departmentId
    ) {}
    /** Request body for updating a subject: all fields optional; only non-null are updated. */
    record UpdateSubjectRequest(String chineseName, String englishName, String description, UUID departmentId) {}
    /** Request body for creating an assessment type: code and chineseName required; englishName, isGraded, isFinal, sortOrder optional with defaults. */
    record CreateAssessmentTypeRequest(
            @NotBlank(message = "Code is required") String code,
            @NotBlank(message = "Chinese name is required") String chineseName,
            String englishName,
            Boolean isGraded,
            Boolean isFinal,
            Integer sortOrder
    ) {}
    /** Request body for updating an assessment type: all fields optional. */
    record UpdateAssessmentTypeRequest(
            String chineseName,
            String englishName,
            Boolean isGraded,
            Boolean isFinal,
            Integer sortOrder
    ) {}
}
