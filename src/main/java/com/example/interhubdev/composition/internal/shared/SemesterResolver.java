package com.example.interhubdev.composition.internal.shared;

import com.example.interhubdev.academic.AcademicApi;
import com.example.interhubdev.academic.SemesterDto;
import com.example.interhubdev.error.Errors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

/**
 * Shared resolver: resolve semester by ID or fallback to current semester by date.
 * Use in composition use-case services to avoid duplicating semester resolution.
 */
@Component
@RequiredArgsConstructor
public class SemesterResolver {

    private final AcademicApi academicApi;

    /**
     * Resolve semester: if semesterId is present, look up by ID; otherwise use current semester (by today's date).
     *
     * @param semesterId optional semester ID
     * @return semester DTO
     * @throws com.example.interhubdev.error.AppException NOT_FOUND if semester not found
     */
    public SemesterDto resolve(Optional<UUID> semesterId) {
        if (semesterId != null && semesterId.isPresent()) {
            return academicApi.findSemesterById(semesterId.get())
                    .orElseThrow(() -> Errors.notFound("Semester not found"));
        }
        return academicApi.findSemesterByDate(LocalDate.now())
                .orElseThrow(() -> Errors.notFound("Current semester not found"));
    }
}
