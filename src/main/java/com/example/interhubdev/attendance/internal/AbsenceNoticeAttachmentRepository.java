package com.example.interhubdev.attendance.internal;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

/**
 * JPA repository for absence_notice_attachment.
 */
interface AbsenceNoticeAttachmentRepository extends JpaRepository<AbsenceNoticeAttachment, UUID> {

    /**
     * Find all attachments for a notice.
     *
     * @param noticeId notice ID
     * @return list of attachments (ordered by createdAt ASC)
     */
    List<AbsenceNoticeAttachment> findByNoticeIdOrderByCreatedAtAsc(UUID noticeId);

    /**
     * Delete all attachments for a notice.
     *
     * @param noticeId notice ID
     */
    void deleteByNoticeId(UUID noticeId);

    /**
     * Delete attachments by notice ID and file IDs (for replace-list semantics).
     *
     * @param noticeId notice ID
     * @param fileIds  list of file IDs to delete
     */
    void deleteByNoticeIdAndFileIdIn(UUID noticeId, List<String> fileIds);

    /**
     * Find all attachments for the given notices (batch load).
     * Call only when noticeIds is non-empty.
     *
     * @param noticeIds notice IDs
     * @return list of attachments (order not specified; group by noticeId in application layer)
     */
    List<AbsenceNoticeAttachment> findByNoticeIdIn(List<UUID> noticeIds);
}
