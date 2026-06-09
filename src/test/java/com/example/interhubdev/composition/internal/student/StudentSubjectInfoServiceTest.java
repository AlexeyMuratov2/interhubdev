package com.example.interhubdev.composition.internal.student;

import com.example.interhubdev.academic.SemesterDto;
import com.example.interhubdev.attendancerecord.AttendanceRecordApi;
import com.example.interhubdev.attendancerecord.AttendanceRecordDto;
import com.example.interhubdev.attendancerecord.AttendanceStatus;
import com.example.interhubdev.attendancerecord.LessonAttendanceRecordItemDto;
import com.example.interhubdev.attendancerecord.StudentAttendanceRecordsByLessonsDto;
import com.example.interhubdev.composition.StudentSubjectInfoDto;
import com.example.interhubdev.composition.internal.shared.SemesterResolver;
import com.example.interhubdev.department.DepartmentApi;
import com.example.interhubdev.document.CourseMaterialApi;
import com.example.interhubdev.document.HomeworkApi;
import com.example.interhubdev.grades.GradesApi;
import com.example.interhubdev.offering.GroupSubjectOfferingDto;
import com.example.interhubdev.offering.OfferingApi;
import com.example.interhubdev.program.CurriculumSubjectDto;
import com.example.interhubdev.program.ProgramApi;
import com.example.interhubdev.schedule.LessonDto;
import com.example.interhubdev.schedule.ScheduleApi;
import com.example.interhubdev.student.StudentApi;
import com.example.interhubdev.student.StudentDto;
import com.example.interhubdev.subject.SubjectApi;
import com.example.interhubdev.subject.SubjectDto;
import com.example.interhubdev.submission.SubmissionApi;
import com.example.interhubdev.teacher.TeacherApi;
import com.example.interhubdev.user.Role;
import com.example.interhubdev.user.UserApi;
import com.example.interhubdev.user.UserDto;
import com.example.interhubdev.user.UserStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("StudentSubjectInfoService")
class StudentSubjectInfoServiceTest {

    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID STUDENT_ID = UUID.randomUUID();
    private static final UUID GROUP_ID = UUID.randomUUID();
    private static final UUID OFFERING_ID = UUID.randomUUID();
    private static final UUID CURRICULUM_SUBJECT_ID = UUID.randomUUID();
    private static final UUID SUBJECT_ID = UUID.randomUUID();
    private static final UUID SEMESTER_ID = UUID.randomUUID();
    private static final UUID LESSON_ID = UUID.randomUUID();

    @Mock
    private AttendanceRecordApi recordApi;
    @Mock
    private CourseMaterialApi courseMaterialApi;
    @Mock
    private DepartmentApi departmentApi;
    @Mock
    private GradesApi gradesApi;
    @Mock
    private HomeworkApi homeworkApi;
    @Mock
    private OfferingApi offeringApi;
    @Mock
    private ProgramApi programApi;
    @Mock
    private ScheduleApi scheduleApi;
    @Mock
    private SemesterResolver semesterResolver;
    @Mock
    private StudentApi studentApi;
    @Mock
    private SubjectApi subjectApi;
    @Mock
    private SubmissionApi submissionApi;
    @Mock
    private TeacherApi teacherApi;
    @Mock
    private UserApi userApi;

    @InjectMocks
    private StudentSubjectInfoService service;

    @Test
    @DisplayName("calculates stats for requester who is both student and super admin")
    void calculatesStatsForMultiRoleStudentRequester() {
        UserDto requester = new UserDto(
                USER_ID,
                "admin@interhubdev.local",
                List.of(Role.STUDENT, Role.SUPER_ADMIN),
                UserStatus.ACTIVE,
                "Super",
                "Admin",
                null,
                null,
                LocalDateTime.now(),
                LocalDateTime.now(),
                LocalDateTime.now()
        );
        StudentDto student = new StudentDto(
                STUDENT_ID,
                USER_ID,
                "S-1",
                "Student",
                null,
                null,
                2025,
                "G1",
                LocalDateTime.now(),
                LocalDateTime.now()
        );
        GroupSubjectOfferingDto offering = new GroupSubjectOfferingDto(
                OFFERING_ID,
                GROUP_ID,
                CURRICULUM_SUBJECT_ID,
                null,
                null,
                "offline",
                null,
                LocalDateTime.now(),
                LocalDateTime.now()
        );
        CurriculumSubjectDto curriculumSubject = new CurriculumSubjectDto(
                CURRICULUM_SUBJECT_ID,
                UUID.randomUUID(),
                SUBJECT_ID,
                2,
                1,
                18,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                UUID.randomUUID(),
                BigDecimal.ONE,
                LocalDateTime.now(),
                LocalDateTime.now()
        );
        SubjectDto subject = new SubjectDto(
                SUBJECT_ID,
                "WEB",
                "web",
                "Web Development",
                null,
                null,
                LocalDateTime.now(),
                LocalDateTime.now()
        );
        LessonDto lesson = new LessonDto(
                LESSON_ID,
                OFFERING_ID,
                null,
                LocalDate.of(2026, 6, 8),
                LocalTime.of(9, 0),
                LocalTime.of(10, 30),
                null,
                null,
                null,
                "planned",
                LocalDateTime.now(),
                LocalDateTime.now()
        );
        SemesterDto semester = new SemesterDto(
                SEMESTER_ID,
                UUID.randomUUID(),
                2,
                "second semester",
                LocalDate.of(2026, 3, 1),
                LocalDate.of(2026, 6, 30),
                null,
                null,
                18,
                true,
                LocalDateTime.now()
        );
        AttendanceRecordDto attendanceRecord = new AttendanceRecordDto(
                UUID.randomUUID(),
                LESSON_ID,
                STUDENT_ID,
                AttendanceStatus.PRESENT,
                Optional.empty(),
                Optional.empty(),
                USER_ID,
                LocalDateTime.of(2026, 7, 1, 12, 0),
                LocalDateTime.of(2026, 7, 1, 12, 0),
                Optional.empty()
        );

        when(userApi.findById(USER_ID)).thenReturn(Optional.of(requester));
        when(offeringApi.findOfferingById(OFFERING_ID)).thenReturn(Optional.of(offering));
        when(studentApi.findByUserId(USER_ID)).thenReturn(Optional.of(student));
        when(studentApi.getGroupIdsByUserId(USER_ID)).thenReturn(List.of(GROUP_ID));
        when(programApi.findCurriculumSubjectById(CURRICULUM_SUBJECT_ID)).thenReturn(Optional.of(curriculumSubject));
        when(subjectApi.findSubjectById(SUBJECT_ID)).thenReturn(Optional.of(subject));
        when(offeringApi.findSlotsByOfferingId(OFFERING_ID)).thenReturn(List.of());
        when(offeringApi.findTeachersByOfferingId(OFFERING_ID)).thenReturn(List.of());
        when(scheduleApi.findLessonsByOfferingId(OFFERING_ID)).thenReturn(List.of(lesson));
        when(semesterResolver.resolveForLessonDates(eq(Optional.empty()), any())).thenReturn(semester);
        when(recordApi.getStudentAttendanceByLessonIds(STUDENT_ID, List.of(LESSON_ID), USER_ID))
                .thenReturn(new StudentAttendanceRecordsByLessonsDto(List.of(
                        new LessonAttendanceRecordItemDto(LESSON_ID, Optional.of(attendanceRecord))
                )));
        when(homeworkApi.listHomeworkIdsByLessonIds(List.of(LESSON_ID), USER_ID)).thenReturn(List.of());
        when(submissionApi.countSubmittedByAuthorForHomeworkIds(USER_ID, List.of(), USER_ID)).thenReturn(0);
        when(gradesApi.getStudentTotalPoints(STUDENT_ID, OFFERING_ID, USER_ID)).thenReturn(BigDecimal.TEN);
        when(courseMaterialApi.listByOffering(OFFERING_ID, USER_ID)).thenReturn(List.of());

        StudentSubjectInfoDto result = service.execute(OFFERING_ID, USER_ID, Optional.empty());

        assertThat(result.studentId()).isEqualTo(STUDENT_ID);
        assertThat(result.stats().attendancePercent()).isEqualTo(100.0);
        assertThat(result.stats().totalPoints()).isEqualByComparingTo(BigDecimal.TEN);
        verify(recordApi).getStudentAttendanceByLessonIds(STUDENT_ID, List.of(LESSON_ID), USER_ID);
        verify(recordApi, never()).getStudentAttendance(any(), any(), any(), any(), any(), any());
    }
}
