package com.example.interhubdev.attendance.internal;

import com.example.interhubdev.attendance.AbsenceNoticeDto;
import com.example.interhubdev.attendance.AbsenceNoticeStatus;
import com.example.interhubdev.schedule.LessonDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Use case for teachers to get absence notices for a lesson session.
 * Teachers can view notices for sessions they teach.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
class GetSessionAbsenceNoticesUseCase {

    private final AbsenceNoticeRepository noticeRepository;
    private final AbsenceNoticeAttachmentRepository attachmentRepository;
    private final SessionGateway sessionGateway;
    private final AttendanceAccessPolicy accessPolicy;

    /**
     * Get absence notices for a lesson session.
     *
     * @param sessionId      lesson session ID
     * @param requesterId    user ID of requester (must be teacher of session or admin)
     * @param includeCanceled if true, include CANCELED notices; if false, only SUBMITTED
     * @return list of notice DTOs (ordered by submittedAt DESC)
     * @throws com.example.interhubdev.error.AppException if session not found or requester cannot access session
     */
    List<AbsenceNoticeDto> execute(UUID sessionId, UUID requesterId, boolean includeCanceled) {
        // Validate session exists and requester can access it
        LessonDto session = sessionGateway.getSessionById(sessionId)
                .orElseThrow(() -> AttendanceErrors.sessionNotFound(sessionId));
        accessPolicy.ensureCanManageSession(requesterId, session);

        // Load notices with optional status filter
        List<AbsenceNotice> notices = includeCanceled
                ? noticeRepository.findByLessonSessionIdOrderBySubmittedAtDesc(sessionId)
                : noticeRepository.findByLessonSessionIdAndStatus(sessionId, AbsenceNoticeStatus.SUBMITTED);

        return notices.stream()
                .map(notice -> {
                    List<AbsenceNoticeAttachment> attachments = attachmentRepository
                            .findByNoticeIdOrderByCreatedAtAsc(notice.getId());
                    return AbsenceNoticeMappers.toDto(notice, attachments);
                })
                .collect(Collectors.toList());
    }
}
