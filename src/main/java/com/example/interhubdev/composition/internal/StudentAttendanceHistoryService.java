package com.example.interhubdev.composition.internal;

import com.example.interhubdev.attendance.AttendanceRecordDto;
import com.example.interhubdev.attendance.AttendanceStatus;
import com.example.interhubdev.attendance.StudentAttendanceByLessonsDto;
import com.example.interhubdev.attendance.StudentLessonAttendanceItemDto;
import com.example.interhubdev.attendance.StudentNoticeSummaryDto;
import com.example.interhubdev.composition.StudentAttendanceHistoryDto;
import com.example.interhubdev.composition.StudentAttendanceHistoryLessonItemDto;
import com.example.interhubdev.error.Errors;
import com.example.interhubdev.offering.OfferingApi;
import com.example.interhubdev.program.ProgramApi;
import com.example.interhubdev.schedule.LessonDto;
import com.example.interhubdev.schedule.ScheduleApi;
import com.example.interhubdev.student.StudentApi;
import com.example.interhubdev.student.StudentDto;
import com.example.interhubdev.subject.SubjectApi;
import com.example.interhubdev.attendance.AttendanceApi;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Use-case service: aggregates student attendance history for an offering (all lessons + attendance + notices).
 * Batch-loads data to avoid N+1.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
class StudentAttendanceHistoryService {

    private final ScheduleApi scheduleApi;
    private final OfferingApi offeringApi;
    private final ProgramApi programApi;
    private final SubjectApi subjectApi;
    private final StudentApi studentApi;
    private final AttendanceApi attendanceApi;

    StudentAttendanceHistoryDto execute(UUID studentId, UUID offeringId, UUID requesterId) {
        if (requesterId == null) {
            throw Errors.unauthorized("Authentication required");
        }

        var offering = offeringApi.findOfferingById(offeringId)
                .orElseThrow(() -> Errors.notFound("Offering not found: " + offeringId));

        StudentDto student = studentApi.findById(studentId)
                .orElseThrow(() -> Errors.notFound("Student not found: " + studentId));

        List<LessonDto> lessons = scheduleApi.findLessonsByOfferingId(offeringId);

        String subjectName = resolveSubjectName(offering.curriculumSubjectId());

        if (lessons.isEmpty()) {
            return new StudentAttendanceHistoryDto(
                    student,
                    offeringId,
                    subjectName,
                    0,
                    0,
                    List.of()
            );
        }

        List<UUID> lessonIds = lessons.stream().map(LessonDto::id).toList();
        StudentAttendanceByLessonsDto batch = attendanceApi.getStudentAttendanceByLessonIds(studentId, lessonIds, requesterId);

        List<StudentLessonAttendanceItemDto> batchItems = batch.items();
        int missedCount = 0;
        int absenceNoticesSubmittedCount = 0;

        List<StudentAttendanceHistoryLessonItemDto> historyLessons = new ArrayList<>(lessons.size());
        for (int i = 0; i < lessons.size(); i++) {
            LessonDto lesson = lessons.get(i);
            StudentLessonAttendanceItemDto item = i < batchItems.size() ? batchItems.get(i) : null;
            Optional<AttendanceRecordDto> attendance = item != null ? item.record() : Optional.empty();
            List<StudentNoticeSummaryDto> notices = item != null ? item.notices() : List.of();

            if (attendance.isPresent()) {
                AttendanceStatus status = attendance.get().status();
                if (status == AttendanceStatus.ABSENT || status == AttendanceStatus.EXCUSED) {
                    missedCount++;
                }
            }
            absenceNoticesSubmittedCount += notices.size();

            historyLessons.add(new StudentAttendanceHistoryLessonItemDto(lesson, attendance, notices));
        }

        return new StudentAttendanceHistoryDto(
                student,
                offeringId,
                subjectName,
                missedCount,
                absenceNoticesSubmittedCount,
                historyLessons
        );
    }

    private String resolveSubjectName(UUID curriculumSubjectId) {
        return programApi.findCurriculumSubjectById(curriculumSubjectId)
                .flatMap(cs -> subjectApi.findSubjectById(cs.subjectId()))
                .map(s -> s.englishName() != null && !s.englishName().isBlank() ? s.englishName() : s.chineseName())
                .orElse("");
    }
}
