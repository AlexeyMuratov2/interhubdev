package com.example.interhubdev.grades;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Response for GET student grades by offering: entries plus total and breakdown by type.
 */
public record StudentOfferingGradesDto(
    UUID studentId,
    UUID offeringId,
    List<GradeEntryDto> entries,
    BigDecimal totalPoints,
    /** Sum of points per type code (e.g. SEMINAR, HOMEWORK). Only ACTIVE entries. */
    Map<String, BigDecimal> breakdownByType
) {
}
