package com.example.interhubdev.composition.internal;

import com.example.interhubdev.composition.LessonHomeworkSubmissionsDto;
import com.example.interhubdev.document.DocumentApi;
import com.example.interhubdev.document.HomeworkApi;
import com.example.interhubdev.document.HomeworkDto;
import com.example.interhubdev.document.StoredFileDto;
import com.example.interhubdev.grades.GradeEntryDto;
import com.example.interhubdev.grades.GradesApi;
import com.example.interhubdev.group.GroupApi;
import com.example.interhubdev.offering.OfferingApi;
import com.example.interhubdev.student.StudentApi;
import com.example.interhubdev.student.StudentDto;
import com.example.interhubdev.submission.HomeworkSubmissionDto;
import com.example.interhubdev.submission.SubmissionApi;
import com.example.interhubdev.error.Errors;
import com.example.interhubdev.schedule.ScheduleApi;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Use-case service: aggregates homework submissions for a lesson (students × homeworks with submission/points/files).
 * For the lesson screen homework submissions table.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
class LessonHomeworkSubmissionsService {

    private final ScheduleApi scheduleApi;
    private final OfferingApi offeringApi;
    private final GroupApi groupApi;
    private final StudentApi studentApi;
    private final HomeworkApi homeworkApi;
    private final SubmissionApi submissionApi;
    private final GradesApi gradesApi;
    private final DocumentApi documentApi;

    LessonHomeworkSubmissionsDto execute(UUID lessonId, UUID requesterId) {
        if (requesterId == null) {
            throw Errors.unauthorized("Authentication required");
        }

        var lesson = scheduleApi.findLessonById(lessonId)
                .orElseThrow(() -> Errors.notFound("Lesson not found: " + lessonId));

        var offering = offeringApi.findOfferingById(lesson.offeringId())
                .orElseThrow(() -> Errors.notFound("Offering not found: " + lesson.offeringId()));

        var group = groupApi.findGroupById(offering.groupId())
                .orElseThrow(() -> Errors.notFound("Group not found: " + offering.groupId()));

        List<StudentDto> roster = studentApi.findByGroupId(offering.groupId());
        List<HomeworkDto> homeworks = homeworkApi.listByLesson(lessonId, requesterId);

        // (authorUserId, homeworkId) -> submission; authorId in submission is user ID, roster has StudentDto with id (student entity) and userId (user/account)
        Map<UUID, Map<UUID, HomeworkSubmissionDto>> submissionByUserAndHomework = new LinkedHashMap<>();
        List<UUID> allSubmissionIds = new ArrayList<>();

        for (var hw : homeworks) {
            List<HomeworkSubmissionDto> submissions = submissionApi.listByHomework(hw.id(), requesterId);
            for (HomeworkSubmissionDto s : submissions) {
                submissionByUserAndHomework
                        .computeIfAbsent(s.authorId(), k -> new LinkedHashMap<>())
                        .put(hw.id(), s);
                allSubmissionIds.add(s.id());
            }
        }

        Map<UUID, GradeEntryDto> gradeEntryBySubmissionId = gradesApi.getGradeEntriesByHomeworkSubmissionIds(allSubmissionIds, requesterId, null);

        // Resolve file metadata for all submission file ids
        Map<UUID, StoredFileDto> filesById = new LinkedHashMap<>();
        for (var e : submissionByUserAndHomework.values()) {
            for (HomeworkSubmissionDto s : e.values()) {
                for (UUID fileId : s.storedFileIds()) {
                    if (!filesById.containsKey(fileId)) {
                        documentApi.getStoredFile(fileId).ifPresent(f -> filesById.put(fileId, f));
                    }
                }
            }
        }

        List<LessonHomeworkSubmissionsDto.StudentHomeworkRowDto> studentRows = roster.stream()
                .map(student -> {
                    List<LessonHomeworkSubmissionsDto.StudentHomeworkItemDto> items = homeworks.stream()
                            .map(hw -> {
                                // Look up by userId: submission.authorId is user (account) ID, not student entity id
                                HomeworkSubmissionDto sub = submissionByUserAndHomework
                                        .getOrDefault(student.userId(), Map.of())
                                        .get(hw.id());
                                if (sub == null) {
                                    return new LessonHomeworkSubmissionsDto.StudentHomeworkItemDto(
                                            hw.id(), null, null, null, List.of());
                                }
                                GradeEntryDto gradeEntry = gradeEntryBySubmissionId.get(sub.id());
                                BigDecimal points = gradeEntry != null ? gradeEntry.points() : null;
                                List<StoredFileDto> files = sub.storedFileIds().stream()
                                        .map(filesById::get)
                                        .filter(Objects::nonNull)
                                        .toList();
                                return new LessonHomeworkSubmissionsDto.StudentHomeworkItemDto(
                                        hw.id(), sub, points, gradeEntry, files);
                            })
                            .toList();
                    return new LessonHomeworkSubmissionsDto.StudentHomeworkRowDto(student, items);
                })
                .toList();

        return new LessonHomeworkSubmissionsDto(lesson, group, homeworks, studentRows);
    }
}
