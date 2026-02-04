package com.example.interhubdev.academic;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record AcademicYearDto(
    UUID id,
    String name,
    LocalDate startDate,
    LocalDate endDate,
    boolean isCurrent,
    LocalDateTime createdAt
) {
}
