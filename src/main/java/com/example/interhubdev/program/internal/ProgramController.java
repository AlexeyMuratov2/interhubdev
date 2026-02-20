package com.example.interhubdev.program.internal;

import com.example.interhubdev.program.CurriculumDto;
import com.example.interhubdev.program.CurriculumPracticeDto;
import com.example.interhubdev.program.CurriculumStatus;
import com.example.interhubdev.program.CurriculumSubjectAssessmentDto;
import com.example.interhubdev.program.CurriculumSubjectDto;
import com.example.interhubdev.program.PracticeLocation;
import com.example.interhubdev.program.PracticeType;
import com.example.interhubdev.program.ProgramApi;
import com.example.interhubdev.program.ProgramDto;
import com.example.interhubdev.program.SemesterIdResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
    @PreAuthorize("hasAnyRole('MODERATOR', 'ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Create program", description = "Only MODERATOR, ADMIN, SUPER_ADMIN can create programs")
    public ResponseEntity<ProgramDto> createProgram(@Valid @RequestBody CreateProgramRequest request) {
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
    @PreAuthorize("hasAnyRole('MODERATOR', 'ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Update program", description = "Only MODERATOR, ADMIN, SUPER_ADMIN can update programs")
    public ResponseEntity<ProgramDto> updateProgram(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateProgramRequest request
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
    @PreAuthorize("hasAnyRole('MODERATOR', 'ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Delete program", description = "Only MODERATOR, ADMIN, SUPER_ADMIN can delete programs")
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
    @PreAuthorize("hasAnyRole('MODERATOR', 'ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Create curriculum", description = "Only MODERATOR, ADMIN, SUPER_ADMIN can create curricula")
    public ResponseEntity<CurriculumDto> createCurriculum(
            @PathVariable UUID programId,
            @Valid @RequestBody CreateCurriculumRequest request
    ) {
        CurriculumDto dto = programApi.createCurriculum(
                programId,
                request.version(),
                request.startYear(),
                request.endYear(),
                request.isActive() != null ? request.isActive() : true,
                request.notes()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }

    @PutMapping("/curricula/{id}")
    @PreAuthorize("hasAnyRole('MODERATOR', 'ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Update curriculum", description = "Only MODERATOR, ADMIN, SUPER_ADMIN can update curricula")
    public ResponseEntity<CurriculumDto> updateCurriculum(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateCurriculumRequest request
    ) {
        CurriculumDto dto = programApi.updateCurriculum(
                id,
                request.version(),
                request.startYear(),
                request.endYear(),
                request.isActive(),
                request.status(),
                request.notes()
        );
        return ResponseEntity.ok(dto);
    }

    @PostMapping("/curricula/{id}/approve")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Approve curriculum", description = "Only ADMIN, SUPER_ADMIN can approve curricula")
    public ResponseEntity<CurriculumDto> approveCurriculum(
            @PathVariable UUID id,
            @RequestParam UUID approvedBy
    ) {
        CurriculumDto dto = programApi.approveCurriculum(id, approvedBy);
        return ResponseEntity.ok(dto);
    }

    @DeleteMapping("/curricula/{id}")
    @PreAuthorize("hasAnyRole('MODERATOR', 'ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Delete curriculum", description = "Only MODERATOR, ADMIN, SUPER_ADMIN can delete curricula")
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

    @GetMapping("/curricula/{curriculumId}/semester-id")
    @Operation(summary = "Get semester ID by curriculum, course and semester number", description = "Resolves the calendar semester for the given curriculum position. Course 1 = curriculum start year, course 2 = start year + 1, etc. Semester number must be 1 or 2.")
    public ResponseEntity<SemesterIdResponse> getSemesterIdForCurriculumCourseAndSemester(
            @PathVariable UUID curriculumId,
            @RequestParam @Min(value = 1, message = "courseYear must be at least 1") int courseYear,
            @RequestParam @Min(value = 1, message = "semesterNo must be 1 or 2") @Max(value = 2, message = "semesterNo must be 1 or 2") int semesterNo
    ) {
        UUID semesterId = programApi.getSemesterIdForCurriculumCourseAndSemester(curriculumId, courseYear, semesterNo);
        return ResponseEntity.ok(new SemesterIdResponse(semesterId));
    }

    @GetMapping("/curriculum-subjects/{id}")
    @Operation(summary = "Get curriculum subject by ID")
    public ResponseEntity<CurriculumSubjectDto> findCurriculumSubjectById(@PathVariable UUID id) {
        return programApi.findCurriculumSubjectById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/curricula/{curriculumId}/subjects")
    @PreAuthorize("hasAnyRole('MODERATOR', 'ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Create curriculum subject", description = "Only MODERATOR, ADMIN, SUPER_ADMIN can create curriculum subjects")
    public ResponseEntity<CurriculumSubjectDto> createCurriculumSubject(
            @PathVariable UUID curriculumId,
            @Valid @RequestBody CreateCurriculumSubjectRequest request
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
                request.hoursSeminar(),
                request.hoursSelfStudy(),
                request.hoursConsultation(),
                request.hoursCourseWork(),
                request.assessmentTypeId(),
                request.credits()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }

    @PutMapping("/curriculum-subjects/{id}")
    @PreAuthorize("hasAnyRole('MODERATOR', 'ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Update curriculum subject", description = "Only MODERATOR, ADMIN, SUPER_ADMIN can update curriculum subjects")
    public ResponseEntity<CurriculumSubjectDto> updateCurriculumSubject(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateCurriculumSubjectRequest request
    ) {
        CurriculumSubjectDto dto = programApi.updateCurriculumSubject(
                id,
                request.courseYear(),
                request.hoursTotal(),
                request.hoursLecture(),
                request.hoursPractice(),
                request.hoursLab(),
                request.hoursSeminar(),
                request.hoursSelfStudy(),
                request.hoursConsultation(),
                request.hoursCourseWork(),
                request.assessmentTypeId(),
                request.credits()
        );
        return ResponseEntity.ok(dto);
    }

    @DeleteMapping("/curriculum-subjects/{id}")
    @PreAuthorize("hasAnyRole('MODERATOR', 'ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Delete curriculum subject", description = "Only MODERATOR, ADMIN, SUPER_ADMIN can delete curriculum subjects")
    public ResponseEntity<Void> deleteCurriculumSubject(@PathVariable UUID id) {
        programApi.deleteCurriculumSubject(id);
        return ResponseEntity.noContent().build();
    }

    // --- Curriculum subject assessments ---
    @GetMapping("/curriculum-subjects/{curriculumSubjectId}/assessments")
    @Operation(summary = "Get assessments by curriculum subject ID")
    public ResponseEntity<List<CurriculumSubjectAssessmentDto>> findAssessmentsByCurriculumSubjectId(
            @PathVariable UUID curriculumSubjectId) {
        return ResponseEntity.ok(programApi.findAssessmentsByCurriculumSubjectId(curriculumSubjectId));
    }

    @PostMapping("/curriculum-subjects/{curriculumSubjectId}/assessments")
    @PreAuthorize("hasAnyRole('MODERATOR', 'ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Create curriculum subject assessment", description = "Only MODERATOR, ADMIN, SUPER_ADMIN can create assessments")
    public ResponseEntity<CurriculumSubjectAssessmentDto> createCurriculumSubjectAssessment(
            @PathVariable UUID curriculumSubjectId,
            @Valid @RequestBody CreateCurriculumSubjectAssessmentRequest request
    ) {
        CurriculumSubjectAssessmentDto dto = programApi.createCurriculumSubjectAssessment(
                curriculumSubjectId,
                request.assessmentTypeId(),
                request.weekNumber(),
                request.isFinal() != null && request.isFinal(),
                request.weight(),
                request.notes()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }

    @PutMapping("/curriculum-subject-assessments/{id}")
    @PreAuthorize("hasAnyRole('MODERATOR', 'ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Update curriculum subject assessment", description = "Only MODERATOR, ADMIN, SUPER_ADMIN can update assessments")
    public ResponseEntity<CurriculumSubjectAssessmentDto> updateCurriculumSubjectAssessment(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateCurriculumSubjectAssessmentRequest request
    ) {
        CurriculumSubjectAssessmentDto dto = programApi.updateCurriculumSubjectAssessment(
                id,
                request.assessmentTypeId(),
                request.weekNumber(),
                request.isFinal(),
                request.weight(),
                request.notes()
        );
        return ResponseEntity.ok(dto);
    }

    @DeleteMapping("/curriculum-subject-assessments/{id}")
    @PreAuthorize("hasAnyRole('MODERATOR', 'ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Delete curriculum subject assessment", description = "Only MODERATOR, ADMIN, SUPER_ADMIN can delete assessments")
    public ResponseEntity<Void> deleteCurriculumSubjectAssessment(@PathVariable UUID id) {
        programApi.deleteCurriculumSubjectAssessment(id);
        return ResponseEntity.noContent().build();
    }

    // --- Curriculum practices ---
    @GetMapping("/curricula/{curriculumId}/practices")
    @Operation(summary = "Get practices by curriculum ID")
    public ResponseEntity<List<CurriculumPracticeDto>> findPracticesByCurriculumId(@PathVariable UUID curriculumId) {
        return ResponseEntity.ok(programApi.findPracticesByCurriculumId(curriculumId));
    }

    @PostMapping("/curricula/{curriculumId}/practices")
    @PreAuthorize("hasAnyRole('MODERATOR', 'ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Create curriculum practice", description = "Only MODERATOR, ADMIN, SUPER_ADMIN can create practices")
    public ResponseEntity<CurriculumPracticeDto> createCurriculumPractice(
            @PathVariable UUID curriculumId,
            @Valid @RequestBody CreateCurriculumPracticeRequest request
    ) {
        CurriculumPracticeDto dto = programApi.createCurriculumPractice(
                curriculumId,
                request.practiceType(),
                request.name(),
                request.description(),
                request.semesterNo(),
                request.durationWeeks(),
                request.credits(),
                request.assessmentTypeId(),
                request.locationType(),
                request.supervisorRequired() != null ? request.supervisorRequired() : true,
                request.reportRequired() != null ? request.reportRequired() : true,
                request.notes()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }

    @PutMapping("/curriculum-practices/{id}")
    @PreAuthorize("hasAnyRole('MODERATOR', 'ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Update curriculum practice", description = "Only MODERATOR, ADMIN, SUPER_ADMIN can update practices")
    public ResponseEntity<CurriculumPracticeDto> updateCurriculumPractice(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateCurriculumPracticeRequest request
    ) {
        CurriculumPracticeDto dto = programApi.updateCurriculumPractice(
                id,
                request.practiceType(),
                request.name(),
                request.description(),
                request.semesterNo(),
                request.durationWeeks(),
                request.credits(),
                request.assessmentTypeId(),
                request.locationType(),
                request.supervisorRequired(),
                request.reportRequired(),
                request.notes()
        );
        return ResponseEntity.ok(dto);
    }

    @DeleteMapping("/curriculum-practices/{id}")
    @PreAuthorize("hasAnyRole('MODERATOR', 'ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Delete curriculum practice", description = "Only MODERATOR, ADMIN, SUPER_ADMIN can delete practices")
    public ResponseEntity<Void> deleteCurriculumPractice(@PathVariable UUID id) {
        programApi.deleteCurriculumPractice(id);
        return ResponseEntity.noContent().build();
    }

    record CreateProgramRequest(
            @NotBlank(message = "Code is required") String code,
            @NotBlank(message = "Name is required") String name,
            String description,
            String degreeLevel,
            UUID departmentId
    ) {}
    record UpdateProgramRequest(String name, String description, String degreeLevel, UUID departmentId) {}
    record CreateCurriculumRequest(
            @NotBlank(message = "Version is required") String version,
            @Min(value = 1900, message = "startYear must be at least 1900") @Max(value = 2100, message = "startYear must be at most 2100") int startYear,
            @Min(value = 1900, message = "endYear must be at least 1900") @Max(value = 2100, message = "endYear must be at most 2100") Integer endYear,
            Boolean isActive,
            String notes
    ) {}
    record UpdateCurriculumRequest(
            String version,
            @Min(value = 1900, message = "startYear must be at least 1900") @Max(value = 2100, message = "startYear must be at most 2100") int startYear,
            @Min(value = 1900, message = "endYear must be at least 1900") @Max(value = 2100, message = "endYear must be at most 2100") Integer endYear,
            boolean isActive,
            CurriculumStatus status,
            String notes
    ) {}
    record CreateCurriculumSubjectRequest(
            @NotNull(message = "Subject id is required") UUID subjectId,
            @Min(value = 1, message = "semesterNo must be 1 or 2") @Max(value = 2, message = "semesterNo must be 1 or 2") int semesterNo,
            Integer courseYear,
            @Min(value = 1, message = "durationWeeks must be at least 1") int durationWeeks,
            Integer hoursTotal,
            Integer hoursLecture,
            Integer hoursPractice,
            Integer hoursLab,
            Integer hoursSeminar,
            Integer hoursSelfStudy,
            Integer hoursConsultation,
            Integer hoursCourseWork,
            @NotNull(message = "Assessment type id is required") UUID assessmentTypeId,
            BigDecimal credits
    ) {}
    record UpdateCurriculumSubjectRequest(
            Integer courseYear,
            Integer hoursTotal,
            Integer hoursLecture,
            Integer hoursPractice,
            Integer hoursLab,
            Integer hoursSeminar,
            Integer hoursSelfStudy,
            Integer hoursConsultation,
            Integer hoursCourseWork,
            UUID assessmentTypeId,
            BigDecimal credits
    ) {}
    record CreateCurriculumSubjectAssessmentRequest(
            @NotNull(message = "Assessment type id is required") UUID assessmentTypeId,
            Integer weekNumber,
            Boolean isFinal,
            BigDecimal weight,
            String notes
    ) {}
    record UpdateCurriculumSubjectAssessmentRequest(
            UUID assessmentTypeId,
            Integer weekNumber,
            Boolean isFinal,
            BigDecimal weight,
            String notes
    ) {}
    record CreateCurriculumPracticeRequest(
            @NotNull(message = "Practice type is required") PracticeType practiceType,
            @NotBlank(message = "Name is required") String name,
            String description,
            @Min(value = 1, message = "semesterNo must be 1 or 2") @Max(value = 2, message = "semesterNo must be 1 or 2") int semesterNo,
            @Min(value = 1, message = "durationWeeks must be at least 1") int durationWeeks,
            BigDecimal credits,
            UUID assessmentTypeId,
            PracticeLocation locationType,
            Boolean supervisorRequired,
            Boolean reportRequired,
            String notes
    ) {}
    record UpdateCurriculumPracticeRequest(
            PracticeType practiceType,
            String name,
            String description,
            @Min(value = 1, message = "semesterNo must be 1 or 2") @Max(value = 2, message = "semesterNo must be 1 or 2") Integer semesterNo,
            Integer durationWeeks,
            BigDecimal credits,
            UUID assessmentTypeId,
            PracticeLocation locationType,
            Boolean supervisorRequired,
            Boolean reportRequired,
            String notes
    ) {}
}
