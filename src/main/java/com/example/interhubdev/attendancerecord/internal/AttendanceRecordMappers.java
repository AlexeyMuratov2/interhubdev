package com.example.interhubdev.attendancerecord.internal;

import com.example.interhubdev.attendancerecord.AttendanceRecordDto;
import com.example.interhubdev.attendancerecord.AttendanceStatus;

import java.util.Optional;

/**
 * Entity to DTO mapping for attendance records.
 */
final class AttendanceRecordMappers {

    private AttendanceRecordMappers() {
    }

    static AttendanceRecordDto toDto(AttendanceRecord e) {
        return new AttendanceRecordDto(
                e.getId(),
                e.getLessonSessionId(),
                e.getStudentId(),
                e.getStatus(),
                Optional.ofNullable(e.getMinutesLate()),
                Optional.ofNullable(e.getTeacherComment()).filter(s -> !s.isBlank()),
                e.getMarkedBy(),
                e.getMarkedAt(),
                e.getUpdatedAt(),
                Optional.ofNullable(e.getAbsenceNoticeId())
        );
    }
}
