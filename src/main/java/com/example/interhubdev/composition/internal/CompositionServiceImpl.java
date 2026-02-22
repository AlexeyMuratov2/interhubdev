package com.example.interhubdev.composition.internal;

import com.example.interhubdev.attendance.AttendanceApi;
import com.example.interhubdev.attendance.SessionAttendanceDto;
import com.example.interhubdev.composition.CompositionApi;
import com.example.interhubdev.composition.LessonFullDetailsDto;
import com.example.interhubdev.composition.LessonHomeworkSubmissionsDto;
import com.example.interhubdev.composition.LessonRosterAttendanceDto;
import com.example.interhubdev.document.DocumentApi;
import com.example.interhubdev.document.HomeworkApi;
import com.example.interhubdev.document.HomeworkDto;
import com.example.interhubdev.document.LessonMaterialApi;
import com.example.interhubdev.document.StoredFileDto;
import com.example.interhubdev.grades.GradeEntryDto;
import com.example.interhubdev.grades.GradesApi;
import com.example.interhubdev.grades.LessonGradesSummaryDto;
import com.example.interhubdev.error.Errors;
import com.example.interhubdev.submission.HomeworkSubmissionDto;
import com.example.interhubdev.submission.SubmissionApi;
import com.example.interhubdev.group.GroupApi;
import com.example.interhubdev.offering.OfferingApi;
import com.example.interhubdev.student.StudentApi;
import com.example.interhubdev.student.StudentDto;
import com.example.interhubdev.offering.OfferingSlotDto;
import com.example.interhubdev.program.ProgramApi;
import com.example.interhubdev.schedule.RoomDto;
import com.example.interhubdev.schedule.ScheduleApi;
import com.example.interhubdev.subject.SubjectApi;
import com.example.interhubdev.teacher.TeacherApi;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service implementing CompositionApi: aggregates data from multiple modules.
 * Read-only composition: no business logic, only data aggregation.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
class CompositionServiceImpl implements CompositionApi {

    private final ScheduleApi scheduleApi;
    private final OfferingApi offeringApi;
    private final SubjectApi subjectApi;
    private final GroupApi groupApi;
    private final LessonMaterialApi lessonMaterialApi;
    private final HomeworkApi homeworkApi;
    private final TeacherApi teacherApi;
    private final ProgramApi programApi;
    private final StudentApi studentApi;
    private final AttendanceApi attendanceApi;
    private final GradesApi gradesApi;
    private final SubmissionApi submissionApi;
    private final DocumentApi documentApi;

    @Override
    public LessonFullDetailsDto getLessonFullDetails(UUID lessonId, UUID requesterId) {
        if (requesterId == null) {
            throw Errors.unauthorized("Authentication required");
        }

        // 1. Get lesson basic information
        var lesson = scheduleApi.findLessonById(lessonId)
                .orElseThrow(() -> Errors.notFound("Lesson not found: " + lessonId));

        // 2. Get offering information
        var offering = offeringApi.findOfferingById(lesson.offeringId())
                .orElseThrow(() -> Errors.notFound("Offering not found: " + lesson.offeringId()));

        // 3. Get curriculum subject to find subjectId
        var curriculumSubject = programApi.findCurriculumSubjectById(offering.curriculumSubjectId())
                .orElseThrow(() -> Errors.notFound("Curriculum subject not found: " + offering.curriculumSubjectId()));

        // 4. Get subject information
        var subject = subjectApi.findSubjectById(curriculumSubject.subjectId())
                .orElseThrow(() -> Errors.notFound("Subject not found: " + curriculumSubject.subjectId()));

        // 5. Get group information
        var group = groupApi.findGroupById(offering.groupId())
                .orElseThrow(() -> Errors.notFound("Group not found: " + offering.groupId()));

        // 6. Get room information (if roomId is not null)
        RoomDto room = null;
        if (lesson.roomId() != null) {
            room = scheduleApi.findRoomById(lesson.roomId())
                    .orElse(null); // Room might be deleted, but lesson still references it
        }

        // 7. Get main teacher (if teacherId is not null)
        var mainTeacher = offering.teacherId() != null
                ? teacherApi.findById(offering.teacherId()).orElse(null)
                : null;

        // 8. Get offering teachers
        var offeringTeachers = offeringApi.findTeachersByOfferingId(offering.id());

        // 9. Get offering slot (if lesson was generated from a slot)
        OfferingSlotDto offeringSlot = null;
        if (lesson.offeringSlotId() != null) {
            offeringSlot = offeringApi.findSlotsByOfferingId(offering.id()).stream()
                    .filter(s -> s.id().equals(lesson.offeringSlotId()))
                    .findFirst()
                    .orElse(null);
        }

        // 10. Get lesson materials
        var materials = lessonMaterialApi.listByLesson(lessonId, requesterId);

        // 11. Get homework assignments
        var homework = homeworkApi.listByLesson(lessonId, requesterId);

        return new LessonFullDetailsDto(
                lesson,
                subject,
                group,
                offering,
                offeringSlot,
                curriculumSubject,
                room,
                mainTeacher,
                offeringTeachers,
                materials,
                homework
        );
    }

    @Override
    public LessonRosterAttendanceDto getLessonRosterAttendance(UUID lessonId, UUID requesterId, boolean includeCanceled) {
        if (requesterId == null) {
            throw Errors.unauthorized("Authentication required");
        }

        var lesson = scheduleApi.findLessonById(lessonId)
                .orElseThrow(() -> Errors.notFound("Lesson not found: " + lessonId));

        var offering = offeringApi.findOfferingById(lesson.offeringId())
                .orElseThrow(() -> Errors.notFound("Offering not found: " + lesson.offeringId()));

        var group = groupApi.findGroupById(offering.groupId())
                .orElseThrow(() -> Errors.notFound("Group not found: " + offering.groupId()));

        var curriculumSubject = programApi.findCurriculumSubjectById(offering.curriculumSubjectId())
                .orElseThrow(() -> Errors.notFound("Curriculum subject not found: " + offering.curriculumSubjectId()));

        var subject = subjectApi.findSubjectById(curriculumSubject.subjectId())
                .orElseThrow(() -> Errors.notFound("Subject not found: " + curriculumSubject.subjectId()));

        List<StudentDto> roster = studentApi.findByGroupId(offering.groupId());

        SessionAttendanceDto sessionAttendance = attendanceApi.getSessionAttendance(lessonId, requesterId, includeCanceled);

        Map<UUID, SessionAttendanceDto.SessionAttendanceStudentDto> attendanceByStudentId = sessionAttendance.students().stream()
                .collect(Collectors.toMap(SessionAttendanceDto.SessionAttendanceStudentDto::studentId, s -> s));

        LessonGradesSummaryDto lessonGrades = gradesApi.getLessonGradesSummary(lessonId, requesterId);
        Map<UUID, BigDecimal> pointsByStudentId = lessonGrades.rows().stream()
                .collect(Collectors.toMap(LessonGradesSummaryDto.LessonGradeRowDto::studentId, LessonGradesSummaryDto.LessonGradeRowDto::totalPoints));

        List<LessonRosterAttendanceDto.LessonRosterAttendanceRowDto> rows = roster.stream()
                .map(student -> {
                    SessionAttendanceDto.SessionAttendanceStudentDto att = attendanceByStudentId.get(student.id());
                    BigDecimal lessonPoints = pointsByStudentId.getOrDefault(student.id(), BigDecimal.ZERO);
                    if (att == null) {
                        return new LessonRosterAttendanceDto.LessonRosterAttendanceRowDto(
                                student,
                                null,
                                null,
                                null,
                                null,
                                null,
                                Optional.empty(),
                                List.of(),
                                lessonPoints
                        );
                    }
                    return new LessonRosterAttendanceDto.LessonRosterAttendanceRowDto(
                            student,
                            att.status(),
                            att.minutesLate(),
                            att.teacherComment(),
                            att.markedAt(),
                            att.markedBy(),
                            att.absenceNoticeId(),
                            att.notices(),
                            lessonPoints
                    );
                })
                .toList();

        String subjectName = subject.englishName() != null && !subject.englishName().isBlank()
                ? subject.englishName()
                : subject.chineseName();
        return new LessonRosterAttendanceDto(
                lesson,
                group,
                subjectName,
                sessionAttendance.counts(),
                sessionAttendance.unmarkedCount(),
                rows
        );
    }

    @Override
    public LessonHomeworkSubmissionsDto getLessonHomeworkSubmissions(UUID lessonId, UUID requesterId) {
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

        Map<UUID, GradeEntryDto> gradeEntryBySubmissionId = gradesApi.getGradeEntriesByHomeworkSubmissionIds(allSubmissionIds, requesterId);

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
