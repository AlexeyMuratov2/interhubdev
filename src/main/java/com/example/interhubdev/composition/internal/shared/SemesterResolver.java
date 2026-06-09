package com.example.interhubdev.composition.internal.shared;

import com.example.interhubdev.academic.AcademicApi;
import com.example.interhubdev.academic.SemesterDto;
import com.example.interhubdev.error.Errors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
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

    /**
     * Resolve semester for an offering-backed report. If no explicit semester is provided,
     * prefer the semester that contains the offering's lesson dates; fallback to current semester
     * for offerings without generated lessons yet.
     */
    public SemesterDto resolveForLessonDates(Optional<UUID> semesterId, Collection<LocalDate> lessonDates) {
        if (semesterId != null && semesterId.isPresent()) {
            return resolve(semesterId);
        }

        Set<LocalDate> dates = lessonDates == null ? Set.of() : lessonDates.stream()
                .filter(Objects::nonNull)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
        if (!dates.isEmpty()) {
            return academicApi.findSemestersByDates(dates).stream()
                    .min(Comparator.comparing(SemesterDto::startDate))
                    .orElseGet(() -> resolve(Optional.empty()));
        }

        return resolve(Optional.empty());
    }
}
