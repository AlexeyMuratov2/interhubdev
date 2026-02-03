package com.example.interhubdev.program.internal;

import com.example.interhubdev.program.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/programs")
@RequiredArgsConstructor
@Tag(name = "Programs", description = "Programs, curricula, curriculum subjects")
class ProgramController {

    private final ProgramApi programApi;

    @GetMapping
    @Operation(summary = "Get all programs")
    public ResponseEntity<List<ProgramDto>> findAllPrograms() {
        return ResponseEntity.ok(programApi.findAllPrograms());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get program by ID")
    public ResponseEntity<ProgramDto> findProgramById(@PathVariable UUID id) {
        return programApi.findProgramById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('STAFF', 'ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Create program", description = "Only STAFF, ADMIN, SUPER_ADMIN can create programs")
    public ResponseEntity<ProgramDto> createProgram(@RequestBody CreateProgramRequest request) {
        ProgramDto dto = programApi.createProgram(
                request.code(),
                request.name(),
                request.description(),
                request.degreeLevel(),
                request.departmentId()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('STAFF', 'ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Update program", description = "Only STAFF, ADMIN, SUPER_ADMIN can update programs")
    public ResponseEntity<ProgramDto> updateProgram(
            @PathVariable UUID id,
            @RequestBody UpdateProgramRequest request
    ) {
        ProgramDto dto = programApi.updateProgram(
                id,
                request.name(),
                request.description(),
                request.degreeLevel(),
                request.departmentId()
        );
        return ResponseEntity.ok(dto);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('STAFF', 'ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Delete program", description = "Only STAFF, ADMIN, SUPER_ADMIN can delete programs")
    public ResponseEntity<Void> deleteProgram(@PathVariable UUID id) {
        programApi.deleteProgram(id);
        return ResponseEntity.noContent().build();
    }

    // --- Curricula ---
    @GetMapping("/{programId}/curricula")
    @Operation(summary = "Get curricula by program ID")
    public ResponseEntity<List<CurriculumDto>> findCurriculaByProgramId(@PathVariable UUID programId) {
        return ResponseEntity.ok(programApi.findCurriculaByProgramId(programId));
    }

    @GetMapping("/curricula/{id}")
    @Operation(summary = "Get curriculum by ID")
    public ResponseEntity<CurriculumDto> findCurriculumById(@PathVariable UUID id) {
        return programApi.findCurriculumById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{programId}/curricula")
    @PreAuthorize("hasAnyRole('STAFF', 'ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Create curriculum", description = "Only STAFF, ADMIN, SUPER_ADMIN can create curricula")
    public ResponseEntity<CurriculumDto> createCurriculum(
            @PathVariable UUID programId,
            @RequestBody CreateCurriculumRequest request
    ) {
        CurriculumDto dto = programApi.createCurriculum(
                programId,
                request.version(),
                request.startYear(),
                request.isActive() != null ? request.isActive() : true,
                request.notes()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }

    @PutMapping("/curricula/{id}")
    @PreAuthorize("hasAnyRole('STAFF', 'ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Update curriculum", description = "Only STAFF, ADMIN, SUPER_ADMIN can update curricula")
    public ResponseEntity<CurriculumDto> updateCurriculum(
            @PathVariable UUID id,
            @RequestBody UpdateCurriculumRequest request
    ) {
        CurriculumDto dto = programApi.updateCurriculum(
                id,
                request.version(),
                request.startYear(),
                request.isActive(),
                request.notes()
        );
        return ResponseEntity.ok(dto);
    }

    @DeleteMapping("/curricula/{id}")
    @PreAuthorize("hasAnyRole('STAFF', 'ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Delete curriculum", description = "Only STAFF, ADMIN, SUPER_ADMIN can delete curricula")
    public ResponseEntity<Void> deleteCurriculum(@PathVariable UUID id) {
        programApi.deleteCurriculum(id);
        return ResponseEntity.noContent().build();
    }

    // --- Curriculum subjects ---
    @GetMapping("/curricula/{curriculumId}/subjects")
    @Operation(summary = "Get curriculum subjects by curriculum ID")
    public ResponseEntity<List<CurriculumSubjectDto>> findCurriculumSubjectsByCurriculumId(@PathVariable UUID curriculumId) {
        return ResponseEntity.ok(programApi.findCurriculumSubjectsByCurriculumId(curriculumId));
    }

    @GetMapping("/curriculum-subjects/{id}")
    @Operation(summary = "Get curriculum subject by ID")
    public ResponseEntity<CurriculumSubjectDto> findCurriculumSubjectById(@PathVariable UUID id) {
        return programApi.findCurriculumSubjectById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/curricula/{curriculumId}/subjects")
    @PreAuthorize("hasAnyRole('STAFF', 'ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Create curriculum subject", description = "Only STAFF, ADMIN, SUPER_ADMIN can create curriculum subjects")
    public ResponseEntity<CurriculumSubjectDto> createCurriculumSubject(
            @PathVariable UUID curriculumId,
            @RequestBody CreateCurriculumSubjectRequest request
    ) {
        CurriculumSubjectDto dto = programApi.createCurriculumSubject(
                curriculumId,
                request.subjectId(),
                request.semesterNo(),
                request.courseYear(),
                request.durationWeeks(),
                request.hoursTotal(),
                request.hoursLecture(),
                request.hoursPractice(),
                request.hoursLab(),
                request.assessmentTypeId(),
                request.isElective() != null && request.isElective(),
                request.credits()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }

    @PutMapping("/curriculum-subjects/{id}")
    @PreAuthorize("hasAnyRole('STAFF', 'ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Update curriculum subject", description = "Only STAFF, ADMIN, SUPER_ADMIN can update curriculum subjects")
    public ResponseEntity<CurriculumSubjectDto> updateCurriculumSubject(
            @PathVariable UUID id,
            @RequestBody UpdateCurriculumSubjectRequest request
    ) {
        CurriculumSubjectDto dto = programApi.updateCurriculumSubject(
                id,
                request.courseYear(),
                request.hoursTotal(),
                request.hoursLecture(),
                request.hoursPractice(),
                request.hoursLab(),
                request.assessmentTypeId(),
                request.isElective(),
                request.credits()
        );
        return ResponseEntity.ok(dto);
    }

    @DeleteMapping("/curriculum-subjects/{id}")
    @PreAuthorize("hasAnyRole('STAFF', 'ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Delete curriculum subject", description = "Only STAFF, ADMIN, SUPER_ADMIN can delete curriculum subjects")
    public ResponseEntity<Void> deleteCurriculumSubject(@PathVariable UUID id) {
        programApi.deleteCurriculumSubject(id);
        return ResponseEntity.noContent().build();
    }

    record CreateProgramRequest(String code, String name, String description, String degreeLevel, UUID departmentId) {}
    record UpdateProgramRequest(String name, String description, String degreeLevel, UUID departmentId) {}
    record CreateCurriculumRequest(String version, int startYear, Boolean isActive, String notes) {}
    record UpdateCurriculumRequest(String version, int startYear, boolean isActive, String notes) {}
    record CreateCurriculumSubjectRequest(
            UUID subjectId, int semesterNo, Integer courseYear, int durationWeeks,
            Integer hoursTotal, Integer hoursLecture, Integer hoursPractice, Integer hoursLab,
            UUID assessmentTypeId, Boolean isElective, BigDecimal credits
    ) {}
    record UpdateCurriculumSubjectRequest(
            Integer courseYear, Integer hoursTotal, Integer hoursLecture, Integer hoursPractice, Integer hoursLab,
            UUID assessmentTypeId, Boolean isElective, BigDecimal credits
    ) {}
}
