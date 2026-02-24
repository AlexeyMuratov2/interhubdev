package com.example.interhubdev.composition.internal;

import com.example.interhubdev.attendance.AttendanceApi;
import com.example.interhubdev.attendance.SessionAttendanceDto;
import com.example.interhubdev.composition.LessonRosterAttendanceDto;
import com.example.interhubdev.error.Errors;
import com.example.interhubdev.grades.GradesApi;
import com.example.interhubdev.grades.LessonGradesSummaryDto;
import com.example.interhubdev.group.GroupApi;
import com.example.interhubdev.offering.OfferingApi;
import com.example.interhubdev.program.ProgramApi;
import com.example.interhubdev.schedule.ScheduleApi;
import com.example.interhubdev.student.StudentApi;
import com.example.interhubdev.student.StudentDto;
import com.example.interhubdev.subject.SubjectApi;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Use-case service: aggregates roster attendance for a lesson (students + attendance status + notices).
 * For the lesson screen attendance table UI.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
class LessonRosterAttendanceService {

    private final ScheduleApi scheduleApi;
    private final OfferingApi offeringApi;
    private final SubjectApi subjectApi;
    private final GroupApi groupApi;
    private final ProgramApi programApi;
    private final StudentApi studentApi;
    private final AttendanceApi attendanceApi;
    private final GradesApi gradesApi;

    LessonRosterAttendanceDto execute(UUID lessonId, UUID requesterId, boolean includeCanceled) {
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
}
