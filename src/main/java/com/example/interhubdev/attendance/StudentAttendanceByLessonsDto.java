package com.example.interhubdev.attendance;

import java.util.List;

/**
 * Batch response: attendance records and absence notices for a student across multiple lessons (merged).
 */
public record StudentAttendanceByLessonsDto(
        List<StudentLessonAttendanceItemDto> items
) {
}
