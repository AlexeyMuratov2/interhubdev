package com.example.interhubdev.attendance;

import com.example.interhubdev.absencenotice.StudentNoticeSummaryDto;
import com.example.interhubdev.attendancerecord.AttendanceRecordDto;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * One lesson's attendance record and notices (merged type for facade).
 */
public record StudentLessonAttendanceItemDto(
        UUID lessonSessionId,
        Optional<AttendanceRecordDto> record,
        List<StudentNoticeSummaryDto> notices
) {
}
