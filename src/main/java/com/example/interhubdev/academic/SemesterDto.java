package com.example.interhubdev.academic;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record SemesterDto(
    UUID id,
    UUID academicYearId,
    int number,
    String name,
    LocalDate startDate,
    LocalDate endDate,
    LocalDate examStartDate,
    LocalDate examEndDate,
    Integer weekCount,
    boolean isCurrent,
    LocalDateTime createdAt
) {
}
