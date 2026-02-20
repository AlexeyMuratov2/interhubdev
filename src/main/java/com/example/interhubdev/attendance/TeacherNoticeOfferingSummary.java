package com.example.interhubdev.attendance;

import java.util.UUID;

/**
 * Summary of an offering for display in teacher's absence notice list.
 * Includes subject name and full offering details so the frontend can render the page without extra requests.
 */
public record TeacherNoticeOfferingSummary(
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
