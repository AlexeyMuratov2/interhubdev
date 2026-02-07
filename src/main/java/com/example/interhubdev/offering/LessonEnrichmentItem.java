package com.example.interhubdev.offering;

import java.util.List;

/**
 * Enrichment for one lesson: offering, slot (nullable), teachers, subject name.
 * Used by LessonEnrichmentDataPort for batch loading (no N+1).
 *
 * @param offering   offering summary
 * @param slot       slot or null if slotId was null or slot not found
 * @param teachers   teachers attached to the offering (with role)
 * @param subjectName display name of the subject (from curriculum subject); may be null
 */
public record LessonEnrichmentItem(
    GroupSubjectOfferingDto offering,
    OfferingSlotDto slot,
    List<OfferingTeacherDto> teachers,
    String subjectName
) {
}
