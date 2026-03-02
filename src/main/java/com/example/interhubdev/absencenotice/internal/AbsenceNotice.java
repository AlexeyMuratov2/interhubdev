package com.example.interhubdev.absencenotice.internal;

import com.example.interhubdev.absencenotice.AbsenceNoticeStatus;
import com.example.interhubdev.absencenotice.AbsenceNoticeType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * JPA entity for absence_notice. Lessons are linked via absence_notice_lesson.
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

    @Version
    @Column(name = "version")
    private Long version;

    @PreUpdate
    void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
