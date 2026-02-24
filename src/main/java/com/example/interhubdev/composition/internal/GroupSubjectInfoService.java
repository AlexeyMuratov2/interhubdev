package com.example.interhubdev.composition.internal;

import com.example.interhubdev.academic.AcademicApi;
import com.example.interhubdev.academic.SemesterDto;
import com.example.interhubdev.attendance.AttendanceApi;
import com.example.interhubdev.attendance.GroupAttendanceSummaryDto;
import com.example.interhubdev.composition.GroupSubjectInfoDto;
import com.example.interhubdev.composition.GroupSubjectStudentItemDto;
import com.example.interhubdev.document.HomeworkApi;
import com.example.interhubdev.error.Errors;
import com.example.interhubdev.grades.GradesApi;
import com.example.interhubdev.grades.GroupOfferingSummaryDto;
import com.example.interhubdev.grades.GroupOfferingSummaryRow;
import com.example.interhubdev.group.GroupApi;
import com.example.interhubdev.group.GroupLeaderDetailDto;
import com.example.interhubdev.group.GroupMemberDto;
import com.example.interhubdev.group.StudentGroupDto;
import com.example.interhubdev.offering.GroupSubjectOfferingDto;
import com.example.interhubdev.offering.OfferingApi;
import com.example.interhubdev.offering.OfferingSlotDto;
import com.example.interhubdev.offering.OfferingTeacherItemDto;
import com.example.interhubdev.program.CurriculumDto;
import com.example.interhubdev.program.CurriculumSubjectDto;
import com.example.interhubdev.program.ProgramApi;
import com.example.interhubdev.program.ProgramDto;
import com.example.interhubdev.schedule.LessonDto;
import com.example.interhubdev.schedule.ScheduleApi;
import com.example.interhubdev.student.StudentDto;
import com.example.interhubdev.subject.SubjectApi;
import com.example.interhubdev.subject.SubjectDto;
import com.example.interhubdev.submission.HomeworkSubmissionDto;
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
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Use-case service: aggregates group subject info for the teacher's "Group subject info" screen.
 * Only teachers assigned to an offering slot for this group and subject (or admin) can view.
 * Attendance percent is obtained from attendance module (only lessons with at least one mark count).
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
class GroupSubjectInfoService {

    private final AcademicApi academicApi;
    private final AttendanceApi attendanceApi;
    private final GroupApi groupApi;
    private final GradesApi gradesApi;
    private final HomeworkApi homeworkApi;
    private final OfferingApi offeringApi;
    private final ProgramApi programApi;
    private final ScheduleApi scheduleApi;
    private final SubmissionApi submissionApi;
    private final SubjectApi subjectApi;
    private final TeacherApi teacherApi;
    private final UserApi userApi;

    GroupSubjectInfoDto execute(UUID groupId, UUID subjectId, UUID requesterId, Optional<UUID> semesterId) {
        if (requesterId == null) {
            throw Errors.unauthorized("Authentication required");
        }

        UserDto requester = userApi.findById(requesterId)
                .orElseThrow(() -> Errors.unauthorized("Authentication required"));

        StudentGroupDto group = groupApi.findGroupById(groupId)
                .orElseThrow(() -> Errors.notFound("Group not found"));

        List<GroupSubjectOfferingDto> offerings = offeringApi.findOfferingsByGroupId(groupId);
        if (offerings.isEmpty()) {
            throw Errors.notFound("Offering not found for this group and subject");
        }

        Collection<UUID> curriculumSubjectIds = offerings.stream()
                .map(GroupSubjectOfferingDto::curriculumSubjectId)
                .collect(Collectors.toSet());
        List<CurriculumSubjectDto> curriculumSubjectsList = programApi.findCurriculumSubjectsByIds(curriculumSubjectIds);
        Map<UUID, CurriculumSubjectDto> curriculumSubjectById = curriculumSubjectsList.stream()
                .collect(Collectors.toMap(CurriculumSubjectDto::id, cs -> cs));

        GroupSubjectOfferingDto offering = null;
        for (GroupSubjectOfferingDto o : offerings) {
            CurriculumSubjectDto cs = curriculumSubjectById.get(o.curriculumSubjectId());
            if (cs != null && cs.subjectId().equals(subjectId) && cs.curriculumId().equals(group.curriculumId())) {
                offering = o;
                break;
            }
        }
        if (offering == null) {
            throw Errors.notFound("Offering not found for this group and subject");
        }

        boolean allowed = false;
        if (requester.hasRole(Role.ADMIN) || requester.hasRole(Role.MODERATOR) || requester.hasRole(Role.SUPER_ADMIN)) {
            allowed = true;
        } else if (requester.hasRole(Role.TEACHER)) {
            TeacherDto teacher = teacherApi.findByUserId(requesterId)
                    .orElseThrow(() -> Errors.forbidden("Only teacher of this subject and group can view"));
            if (Objects.equals(offering.teacherId(), teacher.id())) {
                allowed = true;
            } else {
                List<OfferingSlotDto> slots = offeringApi.findSlotsByOfferingId(offering.id());
                allowed = slots.stream().anyMatch(s -> Objects.equals(s.teacherId(), teacher.id()));
            }
        }
        if (!allowed) {
            throw Errors.forbidden("Only teacher of this subject and group can view");
        }

        SemesterDto semester;
        if (semesterId != null && semesterId.isPresent()) {
            semester = academicApi.findSemesterById(semesterId.get())
                    .orElseThrow(() -> Errors.notFound("Semester not found"));
        } else {
            semester = academicApi.findSemesterByDate(LocalDate.now())
                    .orElseThrow(() -> Errors.notFound("Current semester not found"));
        }
        LocalDate from = semester.startDate();
        LocalDate to = semester.endDate();

        SubjectDto subject = subjectApi.findSubjectById(subjectId)
                .orElseThrow(() -> Errors.notFound("Subject not found"));
        List<OfferingSlotDto> slots = offeringApi.findSlotsByOfferingId(offering.id());
        List<OfferingTeacherItemDto> teachers = offeringApi.findTeachersByOfferingId(offering.id());
        CurriculumSubjectDto curriculumSubject = curriculumSubjectById.get(offering.curriculumSubjectId());
        if (curriculumSubject == null) {
            throw Errors.notFound("Curriculum subject not found");
        }
        CurriculumDto curriculum = programApi.findCurriculumById(group.curriculumId())
                .orElseThrow(() -> Errors.notFound("Curriculum not found"));
        ProgramDto program = programApi.findProgramById(group.programId())
                .orElseThrow(() -> Errors.notFound("Program not found"));
        List<CurriculumSubjectDto> curriculumSubjects = programApi.findCurriculumSubjectsByCurriculumId(group.curriculumId());

        List<GroupMemberDto> members = groupApi.getGroupMembersWithUsers(groupId);
        List<GroupLeaderDetailDto> leaders = groupApi.findLeadersByGroupId(groupId);
        GroupOfferingSummaryDto grades = gradesApi.getGroupOfferingSummary(
                groupId, offering.id(), from.atStartOfDay(), to.atTime(23, 59, 59), false, requesterId);
        GroupAttendanceSummaryDto attendance = attendanceApi.getGroupAttendanceSummary(
                groupId, from, to, offering.id(), requesterId);

        List<LessonDto> lessons = scheduleApi.findLessonsByOfferingId(offering.id());
        List<UUID> lessonIdsInRange = lessons.stream()
                .filter(l -> !l.date().isBefore(from) && !l.date().isAfter(to))
                .map(LessonDto::id)
                .toList();
        List<UUID> homeworkIds = homeworkApi.listHomeworkIdsByLessonIds(lessonIdsInRange, requesterId);
        int totalHomeworkCount = homeworkIds.size();
        List<HomeworkSubmissionDto> submissions = submissionApi.listByHomeworkIds(homeworkIds, requesterId);
        Map<UUID, Long> submittedCountByUserId = submissions.stream()
                .collect(Collectors.groupingBy(HomeworkSubmissionDto::authorId, Collectors.mapping(HomeworkSubmissionDto::homeworkId, Collectors.collectingAndThen(Collectors.toSet(), s -> (long) s.size()))));

        Map<UUID, BigDecimal> pointsByStudentId = grades.rows().stream()
                .collect(Collectors.toMap(GroupOfferingSummaryRow::studentId, GroupOfferingSummaryRow::totalPoints));
        Map<UUID, GroupAttendanceSummaryDto.GroupAttendanceRowDto> attendanceByStudentId = attendance.rows().stream()
                .collect(Collectors.toMap(GroupAttendanceSummaryDto.GroupAttendanceRowDto::studentId, r -> r));

        List<GroupSubjectStudentItemDto> studentItems = new ArrayList<>();
        for (GroupMemberDto member : members) {
            StudentDto student = member.student();
            UserDto user = member.user();
            BigDecimal totalPoints = pointsByStudentId.getOrDefault(student.id(), BigDecimal.ZERO);
            int submittedHomeworkCount = submittedCountByUserId.getOrDefault(user.id(), 0L).intValue();
            GroupAttendanceSummaryDto.GroupAttendanceRowDto attRow = attendanceByStudentId.get(student.id());
            Double attendancePercent = attRow != null ? attRow.attendancePercent() : null;
            studentItems.add(new GroupSubjectStudentItemDto(student, user, totalPoints, submittedHomeworkCount, attendancePercent));
        }

        return new GroupSubjectInfoDto(
                subject,
                group,
                leaders,
                program,
                offering,
                slots,
                teachers,
                curriculumSubject,
                curriculum,
                curriculumSubjects,
                semester,
                totalHomeworkCount,
                studentItems
        );
    }
}
