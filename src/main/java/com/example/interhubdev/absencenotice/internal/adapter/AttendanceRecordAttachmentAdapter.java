package com.example.interhubdev.absencenotice.internal.adapter;

import com.example.interhubdev.absencenotice.AttendanceRecordAttachmentPort;
import com.example.interhubdev.attendancerecord.AttendanceRecordApi;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Adapter that delegates attachment updates to the attendance-record module.
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
}
