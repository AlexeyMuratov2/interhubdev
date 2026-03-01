package com.example.interhubdev.absencenotice;

import java.util.UUID;

/**
 * Summary of an offering for display in teacher's absence notice list.
 */
public record TeacherNoticeOfferingSummary(
        UUID id,
        UUID groupId,
        UUID curriculumSubjectId,
        String subjectName,
        String format,
        String notes
) {
}
