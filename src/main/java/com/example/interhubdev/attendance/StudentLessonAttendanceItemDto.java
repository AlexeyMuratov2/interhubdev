package com.example.interhubdev.attendance;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Attendance and absence notices for one student in one lesson (batch response item).
 */
public record StudentLessonAttendanceItemDto(
        UUID lessonSessionId,
        Optional<AttendanceRecordDto> record,
        List<StudentNoticeSummaryDto> notices
) {
}
