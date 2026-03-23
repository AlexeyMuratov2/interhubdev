package com.example.interhubdev.submission.internal;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface SubmissionAttachmentRepository extends JpaRepository<SubmissionAttachment, UUID> {

    List<SubmissionAttachment> findBySubmissionIdOrderBySortOrderAsc(UUID submissionId);

    @Query("SELECT sa FROM SubmissionAttachment sa WHERE sa.submissionId IN :submissionIds ORDER BY sa.submissionId, sa.sortOrder")
    List<SubmissionAttachment> findBySubmissionIdInOrderBySubmissionIdAndSortOrder(@Param("submissionIds") Collection<UUID> submissionIds);

    Optional<SubmissionAttachment> findById(UUID id);

    long countByFileAssetId(UUID fileAssetId);

    void deleteBySubmissionId(UUID submissionId);
}
