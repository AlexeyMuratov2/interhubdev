package com.example.interhubdev.attendancerecord;

import java.util.List;

/**
 * Batch response: attendance records for a student across multiple lessons (records only).
 * One item per lesson in the requested order.
 */
public record StudentAttendanceRecordsByLessonsDto(
        List<LessonAttendanceRecordItemDto> items
) {
}
