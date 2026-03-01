package com.example.interhubdev.absencenotice;

import com.example.interhubdev.error.AppException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Public API for absence notices: create, update, cancel, respond, list, attach/detach to record.
 */
public interface AbsenceNoticeApi {

    AbsenceNoticeDto createAbsenceNotice(SubmitAbsenceNoticeRequest request, UUID studentId);

    AbsenceNoticeDto updateAbsenceNotice(UUID noticeId, SubmitAbsenceNoticeRequest request, UUID studentId);

    AbsenceNoticeDto cancelAbsenceNotice(UUID noticeId, UUID studentId);

    AbsenceNoticeDto respondToAbsenceNotice(UUID noticeId, boolean approved, String comment, UUID teacherId);

    TeacherAbsenceNoticePage getTeacherAbsenceNotices(
            UUID teacherId,
            List<AbsenceNoticeStatus> statuses,
            UUID cursor,
            Integer limit
    );

    StudentAbsenceNoticePage getMyAbsenceNotices(
            UUID studentId,
            LocalDateTime from,
            LocalDateTime to,
            UUID cursor,
            Integer limit
    );

    /**
     * Get notices for a lesson session grouped by student ID (for session attendance merge).
     *
     * @param sessionId      lesson session ID
     * @param includeCanceled if true, include CANCELED notices
     * @return map of studentId to list of notice summaries (minimal DTOs for session row)
     */
    Map<UUID, List<StudentNoticeSummaryDto>> getSessionNotices(UUID sessionId, boolean includeCanceled);

    /**
     * Get full notice DTOs for a lesson session (for teacher session notices endpoint).
     */
    List<AbsenceNoticeDto> getSessionNoticesAsList(UUID sessionId, boolean includeCanceled);

    /**
     * Get notices for a student and list of lesson IDs (for batch merge in composition/facade).
     *
     * @param studentId  student profile ID
     * @param lessonIds  lesson (session) IDs
     * @return map of lessonSessionId to list of notice summaries
     */
    Map<UUID, List<StudentNoticeSummaryDto>> getNoticesByStudentAndLessons(UUID studentId, List<UUID> lessonIds);

    /**
     * Attach notice to record: update notice.attachedRecordId and call port to set record.absenceNoticeId.
     *
     * @param noticeId    absence notice ID
     * @param recordId    attendance record ID
     * @param requesterId user ID (teacher)
     * @return updated notice DTO
     */
    AbsenceNoticeDto attachToRecord(UUID noticeId, UUID recordId, UUID requesterId);

    /**
     * Detach notice from record: clear notice.attachedRecordId and call port to clear record.absenceNoticeId.
     *
     * @param noticeId    absence notice ID
     * @param requesterId user ID (teacher)
     * @return updated notice DTO
     */
    AbsenceNoticeDto detachFromRecord(UUID noticeId, UUID requesterId);

    /**
     * Detach notice from record by record ID (find notice with attachedRecordId = recordId, then detach).
     *
     * @param recordId    attendance record ID
     * @param requesterId user ID (teacher)
     * @return updated notice DTO if a notice was attached, null if no notice was linked to this record
     */
    AbsenceNoticeDto detachFromRecordByRecordId(UUID recordId, UUID requesterId);

    /**
     * Find last submitted notice for student and session (for auto-attach).
     *
     * @param sessionId lesson session ID
     * @param studentId student profile ID
     * @return optional notice ID
     */
    java.util.Optional<UUID> findLastSubmittedNoticeIdForSessionAndStudent(UUID sessionId, UUID studentId);
}
