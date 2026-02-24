package com.example.interhubdev.composition.internal;

import com.example.interhubdev.composition.StudentGradeHistoryDto;
import com.example.interhubdev.composition.StudentGradeHistoryItemDto;
import com.example.interhubdev.document.HomeworkApi;
import com.example.interhubdev.document.HomeworkDto;
import com.example.interhubdev.error.Errors;
import com.example.interhubdev.grades.GradeEntryDto;
import com.example.interhubdev.grades.GradesApi;
import com.example.interhubdev.grades.StudentOfferingGradesDto;
import com.example.interhubdev.offering.OfferingApi;
import com.example.interhubdev.schedule.LessonDto;
import com.example.interhubdev.schedule.ScheduleApi;
import com.example.interhubdev.submission.HomeworkSubmissionDto;
import com.example.interhubdev.submission.SubmissionApi;
import com.example.interhubdev.user.UserApi;
import com.example.interhubdev.user.UserDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Use-case service: aggregates full grade history for a student in an offering
 * with lesson, homework, submission and grader context. Batch-loads related data to avoid N+1.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
class StudentGradeHistoryService {

    private final GradesApi gradesApi;
    private final ScheduleApi scheduleApi;
    private final SubmissionApi submissionApi;
    private final HomeworkApi homeworkApi;
    private final UserApi userApi;
    private final OfferingApi offeringApi;

    StudentGradeHistoryDto execute(UUID studentId, UUID offeringId, UUID requesterId) {
        if (requesterId == null) {
            throw Errors.unauthorized("Authentication required");
        }

        if (offeringApi.findOfferingById(offeringId).isEmpty()) {
            throw Errors.notFound("Offering not found: " + offeringId);
        }

        StudentOfferingGradesDto grades = gradesApi.getStudentOfferingGrades(
                studentId, offeringId, null, null, true, requesterId);

        List<GradeEntryDto> entries = grades.entries();
        if (entries.isEmpty()) {
            return new StudentGradeHistoryDto(
                    studentId, offeringId,
                    grades.totalPoints(), grades.breakdownByType(),
                    List.of());
        }

        Set<UUID> lessonIds = new HashSet<>();
        Set<UUID> submissionIds = new HashSet<>();
        Set<UUID> gradedByIds = new HashSet<>();
        for (GradeEntryDto e : entries) {
            e.lessonSessionId().ifPresent(lessonIds::add);
            e.homeworkSubmissionId().ifPresent(submissionIds::add);
            gradedByIds.add(e.gradedBy());
        }

        Map<UUID, LessonDto> lessonById = batchLessons(lessonIds);
        Map<UUID, HomeworkSubmissionDto> submissionById = batchSubmissions(submissionIds, requesterId);
        Set<UUID> homeworkIds = submissionById.values().stream()
                .map(HomeworkSubmissionDto::homeworkId)
                .collect(Collectors.toSet());
        Map<UUID, HomeworkDto> homeworkById = batchHomeworks(homeworkIds, requesterId);
        Set<UUID> homeworkLessonIds = homeworkById.values().stream()
                .map(HomeworkDto::lessonId)
                .collect(Collectors.toSet());
        homeworkLessonIds.removeAll(lessonById.keySet());
        Map<UUID, LessonDto> lessonForHomeworkById = batchLessons(homeworkLessonIds);
        Map<UUID, UserDto> userById = batchUsers(gradedByIds);

        List<StudentGradeHistoryItemDto> items = entries.stream()
                .map(entry -> toItem(entry, lessonById, submissionById, homeworkById, userById, lessonForHomeworkById))
                .toList();

        return new StudentGradeHistoryDto(
                studentId, offeringId,
                grades.totalPoints(), grades.breakdownByType(),
                items);
    }

    private Map<UUID, LessonDto> batchLessons(Set<UUID> lessonIds) {
        if (lessonIds.isEmpty()) return Map.of();
        List<LessonDto> list = scheduleApi.findLessonsByIds(lessonIds);
        return list.stream().collect(Collectors.toMap(LessonDto::id, l -> l, (a, b) -> a));
    }

    private Map<UUID, HomeworkSubmissionDto> batchSubmissions(Set<UUID> submissionIds, UUID requesterId) {
        if (submissionIds.isEmpty()) return Map.of();
        List<HomeworkSubmissionDto> list = submissionApi.getByIds(submissionIds, requesterId);
        return list.stream().collect(Collectors.toMap(HomeworkSubmissionDto::id, s -> s, (a, b) -> a));
    }

    private Map<UUID, HomeworkDto> batchHomeworks(Set<UUID> homeworkIds, UUID requesterId) {
        if (homeworkIds.isEmpty()) return Map.of();
        List<HomeworkDto> list = homeworkApi.getByIds(homeworkIds, requesterId);
        return list.stream().collect(Collectors.toMap(HomeworkDto::id, h -> h, (a, b) -> a));
    }

    private Map<UUID, UserDto> batchUsers(Set<UUID> userIds) {
        if (userIds.isEmpty()) return Map.of();
        List<UserDto> list = userApi.findByIds(userIds);
        return list.stream().collect(Collectors.toMap(UserDto::id, u -> u, (a, b) -> a));
    }

    private static StudentGradeHistoryItemDto toItem(
            GradeEntryDto entry,
            Map<UUID, LessonDto> lessonById,
            Map<UUID, HomeworkSubmissionDto> submissionById,
            Map<UUID, HomeworkDto> homeworkById,
            Map<UUID, UserDto> userById,
            Map<UUID, LessonDto> lessonForHomeworkById) {
        Optional<LessonDto> lesson = entry.lessonSessionId().flatMap(id -> Optional.ofNullable(lessonById.get(id)));
        Optional<HomeworkSubmissionDto> submission = entry.homeworkSubmissionId()
                .flatMap(id -> Optional.ofNullable(submissionById.get(id)));
        Optional<HomeworkDto> homework = submission.flatMap(s -> Optional.ofNullable(homeworkById.get(s.homeworkId())));
        Optional<UserDto> gradedByUser = Optional.ofNullable(userById.get(entry.gradedBy()));
        Optional<LessonDto> lessonForHomework = homework.flatMap(h ->
                Optional.ofNullable(lessonById.get(h.lessonId()))
                        .or(() -> Optional.ofNullable(lessonForHomeworkById.get(h.lessonId()))));
        return new StudentGradeHistoryItemDto(entry, lesson, homework, submission, gradedByUser, lessonForHomework);
    }
}
