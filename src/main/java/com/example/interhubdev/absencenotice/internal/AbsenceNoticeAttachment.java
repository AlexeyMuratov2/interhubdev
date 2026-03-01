package com.example.interhubdev.absencenotice.internal;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * JPA entity for absence_notice_attachment.
 */
@Entity
@Table(name = "absence_notice_attachment",
        uniqueConstraints = @UniqueConstraint(columnNames = {"notice_id", "file_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
class AbsenceNoticeAttachment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "notice_id", nullable = false)
    private UUID noticeId;

    @Column(name = "file_id", nullable = false, length = 36)
    private String fileId;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
