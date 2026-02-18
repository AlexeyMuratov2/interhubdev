package com.example.interhubdev.grades;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

/**
 * One row in group offering summary: student id, total points, breakdown by type.
 */
public record GroupOfferingSummaryRow(
    UUID studentId,
    BigDecimal totalPoints,
    Map<String, BigDecimal> breakdownByType
) {
}
