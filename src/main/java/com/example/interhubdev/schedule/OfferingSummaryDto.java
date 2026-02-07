package com.example.interhubdev.schedule;

import java.util.UUID;

/**
 * Minimal offering data for schedule display (no dependency on offering module).
 */
public record OfferingSummaryDto(
    UUID id,
    UUID groupId,
    UUID curriculumSubjectId,
    UUID teacherId
) {
}
