package com.example.interhubdev.program.internal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "curriculum_subject")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
class CurriculumSubject {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "curriculum_id", nullable = false)
    private UUID curriculumId;

    @Column(name = "subject_id", nullable = false)
    private UUID subjectId;

    @Column(name = "semester_no", nullable = false)
    private int semesterNo;

    @Column(name = "course_year")
    private Integer courseYear;

    @Column(name = "duration_weeks", nullable = false)
    private int durationWeeks;

    @Column(name = "hours_total")
    private Integer hoursTotal;

    @Column(name = "hours_lecture")
    private Integer hoursLecture;

    @Column(name = "hours_practice")
    private Integer hoursPractice;

    @Column(name = "hours_lab")
    private Integer hoursLab;

    @Column(name = "hours_seminar")
    private Integer hoursSeminar;

    @Column(name = "hours_self_study")
    private Integer hoursSelfStudy;

    @Column(name = "hours_consultation")
    private Integer hoursConsultation;

    @Column(name = "hours_course_work")
    private Integer hoursCourseWork;

    @Column(name = "assessment_type_id", nullable = false)
    private UUID assessmentTypeId;

    @Column(name = "is_elective")
    @Builder.Default
    private boolean isElective = false;

    @Column(name = "credits", precision = 5, scale = 2)
    private BigDecimal credits;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at", nullable = false)
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();
}
