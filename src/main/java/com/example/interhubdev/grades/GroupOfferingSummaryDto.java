package com.example.interhubdev.grades;

import java.util.List;
import java.util.UUID;

/**
 * Response for GET group offering grades summary.
 */
public record GroupOfferingSummaryDto(
    UUID groupId,
    UUID offeringId,
    List<GroupOfferingSummaryRow> rows
) {
}
