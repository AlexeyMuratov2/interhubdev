package com.example.interhubdev.submission.internal;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.util.UUID;

/**
 * JPA entity for a file attached to a submission.
 * References stored_file in document module (same DB).
 */
@Entity
@Table(name = "homework_submission_file")
@IdClass(HomeworkSubmissionFile.SubmissionFileId.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
class HomeworkSubmissionFile {

    @Id
    @Column(name = "submission_id", nullable = false)
    private UUID submissionId;

    @Id
    @Column(name = "stored_file_id", nullable = false)
    private UUID storedFileId;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "submission_id", insertable = false, updatable = false)
    private HomeworkSubmission submission;

    @EqualsAndHashCode
    @NoArgsConstructor
    @AllArgsConstructor
    static class SubmissionFileId implements Serializable {
        private UUID submissionId;
        private UUID storedFileId;
    }
}
