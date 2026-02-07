package com.example.interhubdev.schedule;

import java.util.List;

/**
 * Lesson with full context for schedule UI: lesson, offering, slot, teachers, room, main teacher, subject name.
 * Returned by GET lessons by date and GET lessons by date for group.
 */
public record LessonForScheduleDto(
    LessonDto lesson,
    OfferingSummaryDto offering,
    SlotSummaryDto slot,
    List<TeacherRoleDto> teachers,
    RoomSummaryDto room,
    TeacherSummaryDto mainTeacher,
    String subjectName
) {
}
