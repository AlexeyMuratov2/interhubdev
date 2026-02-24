package com.example.interhubdev.attendance;

import java.util.List;

/**
 * Batch response: attendance records and absence notices for a student across multiple lessons.
 * One item per lesson (in requested order); missing lessons have empty record and notices.
 */
public record StudentAttendanceByLessonsDto(
        List<StudentLessonAttendanceItemDto> items
) {
}
