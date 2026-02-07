package com.example.interhubdev.schedule;

import java.util.List;

/**
 * Enrichment data for one lesson: offering summary, slot (if any), teachers, subject name.
 * Used by LessonEnrichmentPort; slot may be null for legacy lessons without offeringSlotId.
 */
public record LessonEnrichmentData(
    OfferingSummaryDto offering,
    SlotSummaryDto slot,
    List<TeacherRoleDto> teachers,
    String subjectName
) {
}
