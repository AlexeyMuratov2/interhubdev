package com.example.interhubdev.absencenotice.internal;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

/**
 * JPA repository for absence_notice_attachment.
 */
interface AbsenceNoticeAttachmentRepository extends JpaRepository<AbsenceNoticeAttachment, UUID> {

    List<AbsenceNoticeAttachment> findByNoticeIdOrderByCreatedAtAsc(UUID noticeId);

    void deleteByNoticeId(UUID noticeId);

    void deleteByNoticeIdAndFileIdIn(UUID noticeId, List<String> fileIds);

    List<AbsenceNoticeAttachment> findByNoticeIdIn(List<UUID> noticeIds);
}
