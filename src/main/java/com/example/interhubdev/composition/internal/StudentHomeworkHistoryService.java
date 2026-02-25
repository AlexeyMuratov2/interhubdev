package com.example.interhubdev.composition.internal;

import com.example.interhubdev.composition.StudentHomeworkHistoryDto;
import com.example.interhubdev.composition.StudentHomeworkHistoryItemDto;
import com.example.interhubdev.document.DocumentApi;
import com.example.interhubdev.document.HomeworkApi;
import com.example.interhubdev.document.HomeworkDto;
import com.example.interhubdev.document.StoredFileDto;
import com.example.interhubdev.error.Errors;
import com.example.interhubdev.grades.GradesApi;
import com.example.interhubdev.offering.OfferingApi;
import com.example.interhubdev.program.ProgramApi;
import com.example.interhubdev.schedule.LessonDto;
import com.example.interhubdev.schedule.ScheduleApi;
import com.example.interhubdev.student.StudentApi;
import com.example.interhubdev.student.StudentDto;
import com.example.interhubdev.subject.SubjectApi;
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
 * Use-case service: aggregates full homework history for a student in an offering (all homeworks
 * and student's submission + grade per homework). Batch-loads data to avoid N+1.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
class StudentHomeworkHistoryService {

    private final ScheduleApi scheduleApi;
    private final OfferingApi offeringApi;
    private final HomeworkApi homeworkApi;
    private final SubmissionApi submissionApi;
    private final GradesApi gradesApi;
    private final DocumentApi documentApi;
    private final StudentApi studentApi;
    private final ProgramApi programApi;
    private final SubjectApi subjectApi;

    StudentHomeworkHistoryDto execute(UUID studentId, UUID offeringId, UUID requesterId) {
        if (requesterId == null) {
            throw Errors.unauthorized("Authentication required");
        }

        var offering = offeringApi.findOfferingById(offeringId)
                .orElseThrow(() -> Errors.notFound("Offering not found: " + offeringId));

        StudentDto student = studentApi.findById(studentId)
                .orElseThrow(() -> Errors.notFound("Student not found: " + studentId));

        String subjectName = resolveSubjectName(offering.curriculumSubjectId());

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

        Map<UUID, StoredFileDto> filesById = resolveSubmissionFiles(submissionByHomeworkId.values());

        List<StudentHomeworkHistoryItemDto> items = homeworks.stream()
                .sorted(comparatorByLessonAndHomework(lessonById))
                .map(hw -> toItem(hw, lessonById, submissionByHomeworkId, gradeBySubmissionId, filesById))
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
            Map<UUID, com.example.interhubdev.grades.GradeEntryDto> gradeBySubmissionId,
            Map<UUID, StoredFileDto> filesById) {
        LessonDto lesson = lessonById.get(homework.lessonId());
        if (lesson == null) {
            throw new IllegalStateException("Lesson not found for homework " + homework.id());
        }
        HomeworkSubmissionDto submission = submissionByHomeworkId.get(homework.id());
        Optional<HomeworkSubmissionDto> submissionOpt = Optional.ofNullable(submission);
        Optional<com.example.interhubdev.grades.GradeEntryDto> gradeOpt = submissionOpt
                .flatMap(s -> Optional.ofNullable(gradeBySubmissionId.get(s.id())));
        List<StoredFileDto> submissionFiles = submissionOpt
                .map(s -> s.storedFileIds().stream()
                        .map(filesById::get)
                        .filter(Objects::nonNull)
                        .toList())
                .orElse(List.of());
        return new StudentHomeworkHistoryItemDto(
                homework,
                lesson,
                submissionOpt,
                gradeOpt,
                submissionFiles);
    }

    private Map<UUID, StoredFileDto> resolveSubmissionFiles(Collection<HomeworkSubmissionDto> submissions) {
        Map<UUID, StoredFileDto> filesById = new LinkedHashMap<>();
        for (HomeworkSubmissionDto s : submissions) {
            for (UUID fileId : s.storedFileIds()) {
                if (!filesById.containsKey(fileId)) {
                    documentApi.getStoredFile(fileId).ifPresent(f -> filesById.put(fileId, f));
                }
            }
        }
        return filesById;
    }

    private String resolveSubjectName(UUID curriculumSubjectId) {
        return programApi.findCurriculumSubjectById(curriculumSubjectId)
                .flatMap(cs -> subjectApi.findSubjectById(cs.subjectId()))
                .map(s -> s.englishName() != null && !s.englishName().isBlank() ? s.englishName() : s.chineseName())
                .orElse("");
    }
}
