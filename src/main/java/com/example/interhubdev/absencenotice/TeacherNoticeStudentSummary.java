package com.example.interhubdev.absencenotice;

import java.util.UUID;

/**
 * Summary of a student for display in teacher's absence notice list.
 */
public record TeacherNoticeStudentSummary(
        UUID id,
        String studentId,
        String displayName,
        String groupName
) {
}
