package com.example.interhubdev.absencenotice;

import java.util.UUID;

/**
 * Summary of a student group for display in teacher's absence notice list.
 */
public record TeacherNoticeGroupSummary(
        UUID id,
        String code,
        String name
) {
}
