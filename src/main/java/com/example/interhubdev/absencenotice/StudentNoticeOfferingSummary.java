package com.example.interhubdev.absencenotice;

import java.util.UUID;

/**
 * Summary of an offering for display in student's absence notice list.
 */
public record StudentNoticeOfferingSummary(
        UUID id,
        UUID groupId,
        UUID curriculumSubjectId,
        String subjectName,
        String format,
        String notes
) {
}
