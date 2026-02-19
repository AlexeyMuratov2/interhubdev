package com.example.interhubdev.attendance.internal;

import com.example.interhubdev.attendance.AbsenceNoticeDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Use case for getting student's own absence notices.
 * Returns notices for the authenticated student within an optional date range.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
class GetMyAbsenceNoticesUseCase {

    private final AbsenceNoticeRepository noticeRepository;
    private final AbsenceNoticeAttachmentRepository attachmentRepository;

    /**
     * Get absence notices for a student.
     *
     * @param studentId student profile ID
     * @param from      optional filter: submittedAt >= from (null to ignore)
     * @param to        optional filter: submittedAt <= to (null to ignore)
     * @return list of notice DTOs (ordered by submittedAt DESC)
     */
    List<AbsenceNoticeDto> execute(UUID studentId, LocalDateTime from, LocalDateTime to) {
        List<AbsenceNotice> notices = noticeRepository.findByStudentIdAndSubmittedAtBetween(
                studentId, from, to);

        return notices.stream()
                .map(notice -> {
                    List<AbsenceNoticeAttachment> attachments = attachmentRepository
                            .findByNoticeIdOrderByCreatedAtAsc(notice.getId());
                    return AbsenceNoticeMappers.toDto(notice, attachments);
                })
                .collect(Collectors.toList());
    }
}
