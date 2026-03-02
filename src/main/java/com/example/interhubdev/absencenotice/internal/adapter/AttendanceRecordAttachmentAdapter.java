package com.example.interhubdev.absencenotice.internal.adapter;

import com.example.interhubdev.absencenotice.AttendanceRecordAttachmentPort;
import com.example.interhubdev.attendancerecord.AttendanceRecordApi;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/**
 * Adapter that delegates attachment updates and lookups to the attendance-record module.
 */
@Component
@RequiredArgsConstructor
class AttendanceRecordAttachmentAdapter implements AttendanceRecordAttachmentPort {

    private final AttendanceRecordApi attendanceRecordApi;

    @Override
    public void attachNotice(UUID recordId, UUID noticeId, UUID requesterId) {
        attendanceRecordApi.attachNotice(recordId, noticeId, requesterId);
    }

    @Override
    public void detachNotice(UUID recordId, UUID requesterId) {
        attendanceRecordApi.detachNotice(recordId, requesterId);
    }

    @Override
    public Optional<UUID> getNoticeIdByRecordId(UUID recordId) {
        return attendanceRecordApi.findRecordById(recordId).flatMap(dto -> dto.absenceNoticeId());
    }

    @Override
    public Optional<UUID> getLessonSessionIdByRecordId(UUID recordId) {
        return attendanceRecordApi.findRecordById(recordId).map(dto -> dto.lessonSessionId());
    }
}
