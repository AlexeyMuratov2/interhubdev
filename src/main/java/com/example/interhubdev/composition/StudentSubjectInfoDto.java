package com.example.interhubdev.composition;

import com.example.interhubdev.document.CourseMaterialDto;
import com.example.interhubdev.offering.GroupSubjectOfferingDto;
import com.example.interhubdev.offering.OfferingSlotDto;
import com.example.interhubdev.program.CurriculumSubjectDto;
import com.example.interhubdev.subject.SubjectDto;

import java.util.List;
import java.util.UUID;

/**
 * Aggregated data for the student's "Subject detail" screen.
 * Contains subject info, curriculum info, offering schedule, teacher details, student-specific statistics,
 * and all course materials with files for display and download.
 */
public record StudentSubjectInfoDto(
    /** Current student's profile ID when requester is a student in the offering's group; null for admin. */
    UUID studentId,
    SubjectDto subject,
    /** Department name resolved from subject.departmentId; null if not available. */
    String departmentName,
    CurriculumSubjectDto curriculumSubject,
    GroupSubjectOfferingDto offering,
    /** Weekly schedule slots for this offering. */
    List<OfferingSlotDto> slots,
    /** Teachers assigned to this offering with profile and user display info. */
    List<StudentSubjectTeacherItemDto> teachers,
    /** Student's personal statistics for this subject (attendance, homework, points). */
    StudentSubjectStatsDto stats,
    /** All course materials for this offering (each with file metadata for display/download). */
    List<CourseMaterialDto> materials
) {
}
