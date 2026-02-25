package com.example.interhubdev.composition.internal;

import com.example.interhubdev.academic.AcademicApi;
import com.example.interhubdev.academic.SemesterDto;
import com.example.interhubdev.attendance.AttendanceApi;
import com.example.interhubdev.attendance.AttendanceStatus;
import com.example.interhubdev.attendance.StudentAttendanceDto;
import com.example.interhubdev.composition.StudentSubjectInfoDto;
import com.example.interhubdev.composition.StudentSubjectStatsDto;
import com.example.interhubdev.composition.StudentSubjectTeacherItemDto;
import com.example.interhubdev.department.DepartmentApi;
import com.example.interhubdev.department.DepartmentDto;
import com.example.interhubdev.document.CourseMaterialApi;
import com.example.interhubdev.document.CourseMaterialDto;
import com.example.interhubdev.document.HomeworkApi;
import com.example.interhubdev.error.Errors;
import com.example.interhubdev.grades.GradesApi;
import com.example.interhubdev.offering.GroupSubjectOfferingDto;
import com.example.interhubdev.offering.OfferingApi;
import com.example.interhubdev.offering.OfferingSlotDto;
import com.example.interhubdev.offering.OfferingTeacherItemDto;
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
import com.example.interhubdev.teacher.TeacherDto;
import com.example.interhubdev.user.Role;
import com.example.interhubdev.user.UserApi;
import com.example.interhubdev.user.UserDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Use-case service: aggregates subject detail for a student's "Subject detail" screen.
 * Only students who belong to the offering's group (or admin) can view.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
class StudentSubjectInfoService {

    private final AcademicApi academicApi;
    private final AttendanceApi attendanceApi;
    private final CourseMaterialApi courseMaterialApi;
    private final DepartmentApi departmentApi;
    private final GradesApi gradesApi;
    private final HomeworkApi homeworkApi;
    private final OfferingApi offeringApi;
    private final ProgramApi programApi;
    private final ScheduleApi scheduleApi;
    private final StudentApi studentApi;
    private final SubjectApi subjectApi;
    private final SubmissionApi submissionApi;
    private final TeacherApi teacherApi;
    private final UserApi userApi;

    StudentSubjectInfoDto execute(UUID offeringId, UUID requesterId, Optional<UUID> semesterId) {
        if (requesterId == null) {
            throw Errors.unauthorized("Authentication required");
        }

        UserDto requester = userApi.findById(requesterId)
                .orElseThrow(() -> Errors.unauthorized("Authentication required"));

        GroupSubjectOfferingDto offering = offeringApi.findOfferingById(offeringId)
                .orElseThrow(() -> Errors.notFound("Offering not found"));

        StudentDto student = resolveAndAuthorize(requester, offering.groupId());

        CurriculumSubjectDto curriculumSubject = programApi.findCurriculumSubjectById(offering.curriculumSubjectId())
                .orElseThrow(() -> Errors.notFound("Curriculum subject not found"));

        SubjectDto subject = subjectApi.findSubjectById(curriculumSubject.subjectId())
                .orElseThrow(() -> Errors.notFound("Subject not found"));

        String departmentName = resolveDepartmentName(subject.departmentId());

        List<OfferingSlotDto> slots = offeringApi.findSlotsByOfferingId(offering.id());

        List<StudentSubjectTeacherItemDto> teachers = resolveTeachers(offering);

        SemesterDto semester = resolveSemester(semesterId);
        StudentSubjectStatsDto stats = computeStats(
                student, requester, offering, semester);

        List<CourseMaterialDto> materials = courseMaterialApi.listByOffering(offering.id(), requesterId);

        UUID studentIdForDto = student != null ? student.id() : null;

        return new StudentSubjectInfoDto(
                studentIdForDto,
                subject,
                departmentName,
                curriculumSubject,
                offering,
                slots,
                teachers,
                stats,
                materials
        );
    }

    /**
     * Resolve the student profile and check authorization.
     * Student must belong to the offering's group; admins can view any.
     */
    private StudentDto resolveAndAuthorize(UserDto requester, UUID groupId) {
        boolean isAdmin = requester.hasRole(Role.ADMIN) || requester.hasRole(Role.MODERATOR)
                || requester.hasRole(Role.SUPER_ADMIN);

        if (isAdmin) {
            return null;
        }

        StudentDto student = studentApi.findByUserId(requester.id())
                .orElseThrow(() -> Errors.forbidden("Only students in this group can view"));

        List<UUID> groupIds = studentApi.getGroupIdsByUserId(requester.id());
        if (!groupIds.contains(groupId)) {
            throw Errors.forbidden("Student is not a member of this offering's group");
        }
        return student;
    }

    private String resolveDepartmentName(UUID departmentId) {
        if (departmentId == null) {
            return null;
        }
        return departmentApi.findById(departmentId)
                .map(DepartmentDto::name)
                .orElse(null);
    }

    /**
     * Resolve all teachers assigned to the offering (main + slot teachers) with profile and user data.
     * Batch-loaded by teacher IDs to avoid N+1.
     */
    private List<StudentSubjectTeacherItemDto> resolveTeachers(GroupSubjectOfferingDto offering) {
        List<OfferingTeacherItemDto> offeringTeachers = offeringApi.findTeachersByOfferingId(offering.id());

        Set<UUID> teacherIds = new LinkedHashSet<>();
        if (offering.teacherId() != null) {
            teacherIds.add(offering.teacherId());
        }
        for (OfferingTeacherItemDto ot : offeringTeachers) {
            if (ot.teacherId() != null) {
                teacherIds.add(ot.teacherId());
            }
        }

        if (teacherIds.isEmpty()) {
            return List.of();
        }

        List<TeacherDto> teacherDtos = teacherApi.findByIds(new ArrayList<>(teacherIds));
        Map<UUID, TeacherDto> teacherById = teacherDtos.stream()
                .collect(Collectors.toMap(TeacherDto::id, t -> t));

        Set<UUID> userIds = teacherDtos.stream()
                .map(TeacherDto::userId)
                .collect(Collectors.toSet());
        List<UserDto> users = userApi.findByIds(userIds);
        Map<UUID, UserDto> userByUserId = users.stream()
                .collect(Collectors.toMap(UserDto::id, u -> u));

        Map<UUID, String> roleByTeacherId = offeringTeachers.stream()
                .filter(ot -> ot.teacherId() != null && ot.role() != null)
                .collect(Collectors.toMap(
                        OfferingTeacherItemDto::teacherId,
                        OfferingTeacherItemDto::role,
                        (a, b) -> a));

        List<StudentSubjectTeacherItemDto> result = new ArrayList<>();
        for (UUID teacherId : teacherIds) {
            TeacherDto teacher = teacherById.get(teacherId);
            if (teacher == null) continue;
            UserDto user = userByUserId.get(teacher.userId());
            String role = roleByTeacherId.get(teacherId);
            if (role == null && Objects.equals(offering.teacherId(), teacherId)) {
                role = "MAIN";
            }
            result.add(new StudentSubjectTeacherItemDto(teacher, user, role));
        }
        return result;
    }

    private SemesterDto resolveSemester(Optional<UUID> semesterId) {
        if (semesterId != null && semesterId.isPresent()) {
            return academicApi.findSemesterById(semesterId.get())
                    .orElseThrow(() -> Errors.notFound("Semester not found"));
        }
        return academicApi.findSemesterByDate(LocalDate.now())
                .orElseThrow(() -> Errors.notFound("Current semester not found"));
    }

    /**
     * Compute student-specific statistics: attendance percent, homework counts, total points.
     * Reuses the same patterns as GroupSubjectInfoService.
     */
    private StudentSubjectStatsDto computeStats(
            StudentDto student, UserDto requester,
            GroupSubjectOfferingDto offering, SemesterDto semester) {

        if (student == null) {
            return new StudentSubjectStatsDto(null, 0, 0, BigDecimal.ZERO);
        }

        LocalDate from = semester.startDate();
        LocalDate to = semester.endDate();

        Double attendancePercent = computeAttendancePercent(student, offering, from, to, requester.id());

        List<LessonDto> lessons = scheduleApi.findLessonsByOfferingId(offering.id());
        List<UUID> lessonIdsInRange = lessons.stream()
                .filter(l -> !l.date().isBefore(from) && !l.date().isAfter(to))
                .map(LessonDto::id)
                .toList();
        List<UUID> homeworkIds = homeworkApi.listHomeworkIdsByLessonIds(lessonIdsInRange, requester.id());
        int totalHomeworkCount = homeworkIds.size();
        int submittedHomeworkCount = submissionApi.countSubmittedByAuthorForHomeworkIds(
                requester.id(), homeworkIds, requester.id());

        BigDecimal totalPoints = gradesApi.getStudentTotalPoints(
                student.id(), offering.id(), requester.id());

        return new StudentSubjectStatsDto(attendancePercent, submittedHomeworkCount, totalHomeworkCount, totalPoints);
    }

    private Double computeAttendancePercent(
            StudentDto student, GroupSubjectOfferingDto offering,
            LocalDate from, LocalDate to, UUID requesterId) {
        StudentAttendanceDto attendance = attendanceApi.getStudentAttendance(
                student.id(),
                from.atStartOfDay(),
                to.atTime(23, 59, 59),
                offering.id(),
                null,
                requesterId
        );

        if (attendance.totalMarked() == null || attendance.totalMarked() == 0) {
            return null;
        }

        int present = attendance.summary().getOrDefault(AttendanceStatus.PRESENT, 0);
        int late = attendance.summary().getOrDefault(AttendanceStatus.LATE, 0);
        return ((double) (present + late) / attendance.totalMarked()) * 100.0;
    }
}
