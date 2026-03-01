package com.example.interhubdev.attendancerecord;

import java.util.Optional;
import java.util.UUID;

/**
 * One lesson's attendance record in batch response (record only, no notices).
 */
public record LessonAttendanceRecordItemDto(
        UUID lessonSessionId,
        Optional<AttendanceRecordDto> record
) {
}
