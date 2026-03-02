package com.example.interhubdev.composition.internal.attendance;

import com.example.interhubdev.absencenotice.AbsenceNoticeApi;
import com.example.interhubdev.absencenotice.StudentNoticeSummaryDto;
import com.example.interhubdev.attendancerecord.AttendanceRecordApi;
import com.example.interhubdev.attendancerecord.AttendanceRecordDto;
import com.example.interhubdev.attendancerecord.AttendanceStatus;
import com.example.interhubdev.attendancerecord.LessonAttendanceRecordItemDto;
import com.example.interhubdev.attendancerecord.StudentAttendanceRecordsByLessonsDto;
import com.example.interhubdev.composition.StudentAttendanceHistoryDto;
import com.example.interhubdev.composition.StudentAttendanceHistoryLessonItemDto;
import com.example.interhubdev.composition.StudentAttendanceHistoryQueryApi;
import com.example.interhubdev.composition.internal.shared.SubjectNameResolver;
import com.example.interhubdev.error.Errors;
import com.example.interhubdev.offering.OfferingApi;
import com.example.interhubdev.schedule.LessonDto;
import com.example.interhubdev.schedule.ScheduleApi;
import com.example.interhubdev.student.StudentApi;
import com.example.interhubdev.student.StudentDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Use-case service: aggregates student attendance history for an offering.
 * Implements StudentAttendanceHistoryQueryApi. Uses SubjectNameResolver for subject display name.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
class StudentAttendanceHistoryService implements StudentAttendanceHistoryQueryApi {

    private final ScheduleApi scheduleApi;
    private final OfferingApi offeringApi;
    private final StudentApi studentApi;
    private final AttendanceRecordApi recordApi;
    private final AbsenceNoticeApi noticeApi;
    private final SubjectNameResolver subjectNameResolver;

    @Override
    public StudentAttendanceHistoryDto getStudentAttendanceHistory(UUID studentId, UUID offeringId, UUID requesterId) {
        return execute(studentId, offeringId, requesterId);
    }

    StudentAttendanceHistoryDto execute(UUID studentId, UUID offeringId, UUID requesterId) {
        if (requesterId == null) {
            throw Errors.unauthorized("Authentication required");
        }

        var offering = offeringApi.findOfferingById(offeringId)
                .orElseThrow(() -> Errors.notFound("Offering not found: " + offeringId));

        StudentDto student = studentApi.findById(studentId)
                .orElseThrow(() -> Errors.notFound("Student not found: " + studentId));

        List<LessonDto> lessons = scheduleApi.findLessonsByOfferingId(offeringId);

        String subjectName = subjectNameResolver.resolve(offering.curriculumSubjectId());

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
        StudentAttendanceRecordsByLessonsDto records = recordApi.getStudentAttendanceByLessonIds(studentId, lessonIds, requesterId);
        var noticesByLesson = noticeApi.getNoticesByStudentAndLessons(studentId, lessonIds);

        int missedCount = 0;
        int absenceNoticesSubmittedCount = 0;

        List<StudentAttendanceHistoryLessonItemDto> historyLessons = new ArrayList<>(lessons.size());
        List<LessonAttendanceRecordItemDto> recordItems = records.items();
        for (int i = 0; i < lessons.size(); i++) {
            LessonDto lesson = lessons.get(i);
            LessonAttendanceRecordItemDto recordItem = i < recordItems.size() ? recordItems.get(i) : null;
            Optional<AttendanceRecordDto> attendance = recordItem != null ? recordItem.record() : Optional.empty();
            List<StudentNoticeSummaryDto> notices = noticesByLesson.getOrDefault(lesson.id(), List.of());

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
}
