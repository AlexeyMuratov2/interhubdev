package com.example.interhubdev.absencenotice;

import java.util.UUID;

/**
 * Port for updating the link between an attendance record and an absence notice.
 * Implemented by an adapter that delegates to {@link com.example.interhubdev.attendancerecord.AttendanceRecordApi}.
 */
public interface AttendanceRecordAttachmentPort {

    /**
     * Set the absence notice on the attendance record.
     *
     * @param recordId    attendance record ID
     * @param noticeId    absence notice ID to attach
     * @param requesterId user ID (for authorization)
     */
    void attachNotice(UUID recordId, UUID noticeId, UUID requesterId);

    /**
     * Clear the absence notice link on the attendance record.
     *
     * @param recordId    attendance record ID
     * @param requesterId user ID (for authorization)
     */
    void detachNotice(UUID recordId, UUID requesterId);
}
