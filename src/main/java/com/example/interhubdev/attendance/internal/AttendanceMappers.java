package com.example.interhubdev.attendance.internal;

import com.example.interhubdev.attendance.AttendanceRecordDto;
import com.example.interhubdev.attendance.AttendanceStatus;

import java.util.Optional;

/**
 * Entity to DTO mapping for attendance records. No instantiation.
 */
final class AttendanceMappers {

    private AttendanceMappers() {
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
                e.getUpdatedAt()
        );
    }
}
