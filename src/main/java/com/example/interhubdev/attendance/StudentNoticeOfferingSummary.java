package com.example.interhubdev.attendance;

import java.util.UUID;

/**
 * Summary of an offering for display in student's absence notice list.
 * Includes subject name and format so the frontend can render the page without extra requests.
 */
public record StudentNoticeOfferingSummary(
        UUID id,
        UUID groupId,
        UUID curriculumSubjectId,
        /** Display name of the subject (from curriculum subject). May be null if subject not found. */
        String subjectName,
        /** Format: offline, online, mixed. May be null. */
        String format,
        /** Optional notes. May be null. */
        String notes
) {}
