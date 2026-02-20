package com.example.interhubdev.attendance.internal;

import com.example.interhubdev.attendance.AbsenceNoticeStatus;
import com.example.interhubdev.attendance.AbsenceNoticeType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * JPA entity for absence_notice.
 * Represents a student's notice about absence or lateness for a lesson session.
 */
@Entity
@Table(name = "absence_notice")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
class AbsenceNotice {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "lesson_session_id", nullable = false)
    private UUID lessonSessionId;

    @Column(name = "student_id", nullable = false)
    private UUID studentId;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20)
    private AbsenceNoticeType type;

    @Column(name = "reason_text", columnDefinition = "TEXT")
    private String reasonText;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private AbsenceNoticeStatus status = AbsenceNoticeStatus.SUBMITTED;

    @Column(name = "submitted_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime submittedAt = LocalDateTime.now();

    @Column(name = "updated_at", nullable = false)
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();

    @Column(name = "canceled_at")
    private LocalDateTime canceledAt;

    @Column(name = "attached_record_id")
    private UUID attachedRecordId;

    @Column(name = "teacher_comment", columnDefinition = "TEXT")
    private String teacherComment;

    @Column(name = "responded_at")
    private LocalDateTime respondedAt;

    @Column(name = "responded_by")
    private UUID respondedBy;

    @Version
    @Column(name = "version")
    private Long version;

    @PreUpdate
    void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
