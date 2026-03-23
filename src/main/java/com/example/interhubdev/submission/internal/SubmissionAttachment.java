package com.example.interhubdev.submission.internal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Business binding between a submission and a technical file asset.
 */
@Entity
@Table(
    name = "submission_attachment",
    indexes = {
        @Index(name = "idx_submission_attachment_submission_id", columnList = "submission_id"),
        @Index(name = "idx_submission_attachment_file_asset_id", columnList = "file_asset_id")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
class SubmissionAttachment {

    @Id
    private UUID id;

    @Column(name = "submission_id", nullable = false)
    private UUID submissionId;

    @Column(name = "file_asset_id", nullable = false)
    private UUID fileAssetId;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
