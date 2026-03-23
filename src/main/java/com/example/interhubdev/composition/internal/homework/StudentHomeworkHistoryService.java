package com.example.interhubdev.composition.internal.homework;

import com.example.interhubdev.composition.StudentHomeworkHistoryDto;
import com.example.interhubdev.composition.StudentHomeworkHistoryItemDto;
import com.example.interhubdev.composition.StudentHomeworkHistoryQueryApi;
import com.example.interhubdev.composition.internal.shared.SubjectNameResolver;
import com.example.interhubdev.document.HomeworkApi;
import com.example.interhubdev.document.HomeworkDto;
import com.example.interhubdev.error.Errors;
import com.example.interhubdev.grades.GradesApi;
import com.example.interhubdev.offering.OfferingApi;
import com.example.interhubdev.schedule.LessonDto;
import com.example.interhubdev.schedule.ScheduleApi;
import com.example.interhubdev.student.StudentApi;
import com.example.interhubdev.student.StudentDto;
import com.example.interhubdev.submission.HomeworkSubmissionDto;
import com.example.interhubdev.submission.SubmissionApi;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Use-case service: aggregates full homework history for a student in an offering.
 * Implements StudentHomeworkHistoryQueryApi. Uses SubjectNameResolver for subject display name.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
class StudentHomeworkHistoryService implements StudentHomeworkHistoryQueryApi {

    private final ScheduleApi scheduleApi;
    private final OfferingApi offeringApi;
    private final HomeworkApi homeworkApi;
    private final SubmissionApi submissionApi;
    private final GradesApi gradesApi;
    private final StudentApi studentApi;
    private final SubjectNameResolver subjectNameResolver;

    @Override
    public StudentHomeworkHistoryDto getStudentHomeworkHistory(UUID studentId, UUID offeringId, UUID requesterId) {
        return execute(studentId, offeringId, requesterId);
    }

    StudentHomeworkHistoryDto execute(UUID studentId, UUID offeringId, UUID requesterId) {
        if (requesterId == null) {
            throw Errors.unauthorized("Authentication required");
        }

        var offering = offeringApi.findOfferingById(offeringId)
                .orElseThrow(() -> Errors.notFound("Offering not found: " + offeringId));

        StudentDto student = studentApi.findById(studentId)
                .orElseThrow(() -> Errors.notFound("Student not found: " + studentId));

        String subjectName = subjectNameResolver.resolve(offering.curriculumSubjectId());

        List<LessonDto> lessons = scheduleApi.findLessonsByOfferingId(offeringId);
        if (lessons.isEmpty()) {
            return new StudentHomeworkHistoryDto(student, offeringId, subjectName, List.of());
        }

        List<UUID> lessonIds = lessons.stream().map(LessonDto::id).toList();
        Map<UUID, LessonDto> lessonById = lessons.stream().collect(Collectors.toMap(LessonDto::id, l -> l, (a, b) -> a));

        List<UUID> homeworkIds = homeworkApi.listHomeworkIdsByLessonIds(lessonIds, requesterId);
        if (homeworkIds.isEmpty()) {
            return new StudentHomeworkHistoryDto(student, offeringId, subjectName, List.of());
        }

        List<HomeworkDto> homeworks = homeworkApi.getByIds(homeworkIds, requesterId);
        List<HomeworkSubmissionDto> allSubmissions = submissionApi.listByHomeworkIds(homeworkIds, requesterId);

        UUID studentUserId = student.userId();
        Map<UUID, HomeworkSubmissionDto> submissionByHomeworkId = allSubmissions.stream()
                .filter(s -> studentUserId.equals(s.authorId()))
                .collect(Collectors.toMap(HomeworkSubmissionDto::homeworkId, s -> s, (a, b) ->
                        a.submittedAt().isAfter(b.submittedAt()) ? a : b));

        List<UUID> submissionIds = new ArrayList<>(submissionByHomeworkId.values().stream().map(HomeworkSubmissionDto::id).toList());
        Map<UUID, com.example.interhubdev.grades.GradeEntryDto> gradeBySubmissionId =
                gradesApi.getGradeEntriesByHomeworkSubmissionIds(submissionIds, requesterId, studentId);

        List<StudentHomeworkHistoryItemDto> items = homeworks.stream()
                .sorted(comparatorByLessonAndHomework(lessonById))
                .map(hw -> toItem(hw, lessonById, submissionByHomeworkId, gradeBySubmissionId))
                .toList();

        return new StudentHomeworkHistoryDto(student, offeringId, subjectName, items);
    }

    private static Comparator<HomeworkDto> comparatorByLessonAndHomework(Map<UUID, LessonDto> lessonById) {
        return Comparator.<HomeworkDto, LocalDate>comparing(h -> {
            LessonDto l = lessonById.get(h.lessonId());
            return l != null ? l.date() : LocalDate.MIN;
        }).thenComparing(h -> {
            LessonDto l = lessonById.get(h.lessonId());
            return l != null ? l.startTime() : LocalTime.MIN;
        }).thenComparing(HomeworkDto::createdAt, Comparator.nullsLast(Comparator.naturalOrder()));
    }

    private static StudentHomeworkHistoryItemDto toItem(
            HomeworkDto homework,
            Map<UUID, LessonDto> lessonById,
            Map<UUID, HomeworkSubmissionDto> submissionByHomeworkId,
            Map<UUID, com.example.interhubdev.grades.GradeEntryDto> gradeBySubmissionId) {
        LessonDto lesson = lessonById.get(homework.lessonId());
        if (lesson == null) {
            throw new IllegalStateException("Lesson not found for homework " + homework.id());
        }
        HomeworkSubmissionDto submission = submissionByHomeworkId.get(homework.id());
        Optional<HomeworkSubmissionDto> submissionOpt = Optional.ofNullable(submission);
        Optional<com.example.interhubdev.grades.GradeEntryDto> gradeOpt = submissionOpt
                .flatMap(s -> Optional.ofNullable(gradeBySubmissionId.get(s.id())));
        List<com.example.interhubdev.submission.SubmissionAttachmentDto> submissionFiles = submissionOpt
                .map(HomeworkSubmissionDto::attachments)
                .orElse(List.of());
        return new StudentHomeworkHistoryItemDto(
                homework,
                lesson,
                submissionOpt,
                gradeOpt,
                submissionFiles);
    }
}
