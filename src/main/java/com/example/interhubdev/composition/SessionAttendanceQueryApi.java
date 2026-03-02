package com.example.interhubdev.composition;

import com.example.interhubdev.absencenotice.AbsenceNoticeDto;

import java.util.List;
import java.util.UUID;

/**
 * Read-only query API for session-level attendance (records + notices merged).
 * Used by lesson roster attendance and direct session attendance UI.
 */
public interface SessionAttendanceQueryApi {

    /**
     * Get attendance records for a lesson session with absence notices per student (merged view).
     *
     * @param sessionId      lesson session ID
     * @param requesterId    current authenticated user ID (for authorization)
     * @param includeCanceled whether to include canceled absence notices
     * @return merged DTO with counts and student rows (record + notices per student)
     * @throws com.example.interhubdev.error.AppException NOT_FOUND if session not found, FORBIDDEN if requester cannot view
     */
    SessionAttendanceViewDto getSessionAttendance(UUID sessionId, UUID requesterId, boolean includeCanceled);

    /**
     * Get list of absence notices for a lesson session (teacher view). Access checked via session.
     *
     * @param sessionId      lesson session ID
     * @param requesterId    current authenticated user ID
     * @param includeCanceled whether to include canceled notices
     * @return list of absence notice DTOs for the session
     * @throws com.example.interhubdev.error.AppException NOT_FOUND/FORBIDDEN if no access
     */
    List<AbsenceNoticeDto> getSessionNotices(UUID sessionId, UUID requesterId, boolean includeCanceled);
}
