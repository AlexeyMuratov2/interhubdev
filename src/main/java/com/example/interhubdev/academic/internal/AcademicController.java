package com.example.interhubdev.academic.internal;

import com.example.interhubdev.academic.AcademicApi;
import com.example.interhubdev.academic.AcademicYearDto;
import com.example.interhubdev.academic.SemesterDto;
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

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/academic")
@RequiredArgsConstructor
@Tag(name = "Academic Calendar", description = "Academic years and semesters")
class AcademicController {

    private final AcademicApi academicApi;

    // --- Academic Years ---
    @GetMapping("/years")
    @Operation(summary = "Get all academic years")
    public ResponseEntity<List<AcademicYearDto>> findAllAcademicYears() {
        return ResponseEntity.ok(academicApi.findAllAcademicYears());
    }

    @GetMapping("/years/current")
    @Operation(summary = "Get current academic year")
    public ResponseEntity<AcademicYearDto> findCurrentAcademicYear() {
        return academicApi.findCurrentAcademicYear()
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/years/{id}")
    @Operation(summary = "Get academic year by ID")
    public ResponseEntity<AcademicYearDto> findAcademicYearById(@PathVariable UUID id) {
        return academicApi.findAcademicYearById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/years")
    @PreAuthorize("hasAnyRole('MODERATOR', 'ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Create academic year", description = "Only MODERATOR, ADMIN, SUPER_ADMIN can create academic years")
    public ResponseEntity<AcademicYearDto> createAcademicYear(@Valid @RequestBody CreateAcademicYearRequest request) {
        AcademicYearDto dto = academicApi.createAcademicYear(
                request.name(),
                request.startDate(),
                request.endDate(),
                request.isCurrent() != null && request.isCurrent()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }

    @PutMapping("/years/{id}")
    @PreAuthorize("hasAnyRole('MODERATOR', 'ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Update academic year", description = "Only MODERATOR, ADMIN, SUPER_ADMIN can update academic years")
    public ResponseEntity<AcademicYearDto> updateAcademicYear(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateAcademicYearRequest request
    ) {
        AcademicYearDto dto = academicApi.updateAcademicYear(
                id,
                request.name(),
                request.startDate(),
                request.endDate(),
                request.isCurrent()
        );
        return ResponseEntity.ok(dto);
    }

    @DeleteMapping("/years/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Delete academic year", description = "Only ADMIN, SUPER_ADMIN can delete academic years")
    public ResponseEntity<Void> deleteAcademicYear(@PathVariable UUID id) {
        academicApi.deleteAcademicYear(id);
        return ResponseEntity.noContent().build();
    }

    // --- Semesters ---
    @GetMapping("/years/{academicYearId}/semesters")
    @Operation(summary = "Get semesters by academic year ID")
    public ResponseEntity<List<SemesterDto>> findSemestersByAcademicYearId(@PathVariable UUID academicYearId) {
        return ResponseEntity.ok(academicApi.findSemestersByAcademicYearId(academicYearId));
    }

    @GetMapping("/semesters/current")
    @Operation(summary = "Get current semester")
    public ResponseEntity<SemesterDto> findCurrentSemester() {
        return academicApi.findCurrentSemester()
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/semesters/by-date")
    @Operation(summary = "Get semester by date", description = "Returns the semester that contains the given date (startDate <= date <= endDate)")
    public ResponseEntity<SemesterDto> findSemesterByDate(@RequestParam @NotNull LocalDate date) {
        return academicApi.findSemesterByDate(date)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/semesters/{id}")
    @Operation(summary = "Get semester by ID")
    public ResponseEntity<SemesterDto> findSemesterById(@PathVariable UUID id) {
        return academicApi.findSemesterById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/years/{academicYearId}/semesters")
    @PreAuthorize("hasAnyRole('MODERATOR', 'ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Create semester", description = "Only MODERATOR, ADMIN, SUPER_ADMIN can create semesters")
    public ResponseEntity<SemesterDto> createSemester(
            @PathVariable UUID academicYearId,
            @Valid @RequestBody CreateSemesterRequest request
    ) {
        SemesterDto dto = academicApi.createSemester(
                academicYearId,
                request.number(),
                request.name(),
                request.startDate(),
                request.endDate(),
                request.examStartDate(),
                request.examEndDate(),
                request.weekCount(),
                request.isCurrent() != null && request.isCurrent()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }

    @PutMapping("/semesters/{id}")
    @PreAuthorize("hasAnyRole('MODERATOR', 'ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Update semester", description = "Only MODERATOR, ADMIN, SUPER_ADMIN can update semesters")
    public ResponseEntity<SemesterDto> updateSemester(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateSemesterRequest request
    ) {
        SemesterDto dto = academicApi.updateSemester(
                id,
                request.name(),
                request.startDate(),
                request.endDate(),
                request.examStartDate(),
                request.examEndDate(),
                request.weekCount(),
                request.isCurrent()
        );
        return ResponseEntity.ok(dto);
    }

    @DeleteMapping("/semesters/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Delete semester", description = "Only ADMIN, SUPER_ADMIN can delete semesters")
    public ResponseEntity<Void> deleteSemester(@PathVariable UUID id) {
        academicApi.deleteSemester(id);
        return ResponseEntity.noContent().build();
    }

    // --- Request records ---
    record CreateAcademicYearRequest(
            @NotBlank(message = "Name is required") String name,
            @NotNull(message = "Start date is required") LocalDate startDate,
            @NotNull(message = "End date is required") LocalDate endDate,
            Boolean isCurrent
    ) {}

    record UpdateAcademicYearRequest(
            String name,
            LocalDate startDate,
            LocalDate endDate,
            Boolean isCurrent
    ) {}

    record CreateSemesterRequest(
            @Min(value = 1, message = "Semester number must be at least 1") int number,
            String name,
            @NotNull(message = "Start date is required") LocalDate startDate,
            @NotNull(message = "End date is required") LocalDate endDate,
            LocalDate examStartDate,
            LocalDate examEndDate,
            @Min(value = 1, message = "Week count must be at least 1") @Max(value = 52, message = "Week count must be at most 52") Integer weekCount,
            Boolean isCurrent
    ) {}

    record UpdateSemesterRequest(
            String name,
            LocalDate startDate,
            LocalDate endDate,
            LocalDate examStartDate,
            LocalDate examEndDate,
            @Min(value = 1, message = "Week count must be at least 1") @Max(value = 52, message = "Week count must be at most 52") Integer weekCount,
            Boolean isCurrent
    ) {}
}
