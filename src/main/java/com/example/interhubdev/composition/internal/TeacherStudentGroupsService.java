package com.example.interhubdev.composition.internal;

import com.example.interhubdev.academic.AcademicApi;
import com.example.interhubdev.academic.AcademicYearDto;
import com.example.interhubdev.academic.SemesterDto;
import com.example.interhubdev.composition.TeacherStudentGroupItemDto;
import com.example.interhubdev.composition.TeacherStudentGroupsDto;
import com.example.interhubdev.error.Errors;
import com.example.interhubdev.group.GroupApi;
import com.example.interhubdev.group.StudentGroupDto;
import com.example.interhubdev.offering.GroupSubjectOfferingDto;
import com.example.interhubdev.offering.OfferingApi;
import com.example.interhubdev.offering.OfferingSlotDto;
import com.example.interhubdev.program.CurriculumDto;
import com.example.interhubdev.program.CurriculumSubjectDto;
import com.example.interhubdev.program.ProgramApi;
import com.example.interhubdev.program.ProgramDto;
import com.example.interhubdev.schedule.ScheduleApi;
import com.example.interhubdev.subject.SubjectApi;
import com.example.interhubdev.subject.SubjectDto;
import com.example.interhubdev.student.StudentApi;
import com.example.interhubdev.teacher.TeacherApi;
import com.example.interhubdev.user.UserApi;
import com.example.interhubdev.user.UserDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Use-case service: aggregates student groups where the current teacher has at least one lesson.
 * For teacher dashboard "Student groups" page.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
class TeacherStudentGroupsService {

    private final TeacherApi teacherApi;
    private final OfferingApi offeringApi;
    private final ScheduleApi scheduleApi;
    private final AcademicApi academicApi;
    private final GroupApi groupApi;
    private final ProgramApi programApi;
    private final SubjectApi subjectApi;
    private final StudentApi studentApi;
    private final UserApi userApi;

    TeacherStudentGroupsDto execute(UUID requesterId) {
        if (requesterId == null) {
            throw Errors.unauthorized("Authentication required");
        }
        var teacher = teacherApi.findByUserId(requesterId)
                .orElseThrow(() -> Errors.forbidden("Not a teacher"));

        List<OfferingSlotDto> slots = offeringApi.findSlotsByTeacherId(teacher.id());
        if (slots.isEmpty()) {
            return new TeacherStudentGroupsDto(List.of(), List.of(), List.of(), List.of());
        }
        List<UUID> slotIds = slots.stream().map(OfferingSlotDto::id).toList();
        Set<UUID> slotIdsWithLessons = scheduleApi.findOfferingSlotIdsWithAtLeastOneLesson(slotIds);
        Set<UUID> offeringIdsSet = new LinkedHashSet<>();
        for (OfferingSlotDto slot : slots) {
            if (slotIdsWithLessons.contains(slot.id())) {
                offeringIdsSet.add(slot.offeringId());
            }
        }
        if (offeringIdsSet.isEmpty()) {
            return new TeacherStudentGroupsDto(List.of(), List.of(), List.of(), List.of());
        }
        List<GroupSubjectOfferingDto> offerings = offeringApi.findOfferingsByIds(offeringIdsSet);
        Set<UUID> groupIdsSet = new LinkedHashSet<>();
        Set<UUID> curriculumSubjectIdsSet = new LinkedHashSet<>();
        for (GroupSubjectOfferingDto o : offerings) {
            groupIdsSet.add(o.groupId());
            curriculumSubjectIdsSet.add(o.curriculumSubjectId());
        }
        List<UUID> groupIds = new ArrayList<>(groupIdsSet);
        if (groupIds.isEmpty()) {
            return new TeacherStudentGroupsDto(List.of(), List.of(), List.of(), List.of());
        }

        // Subjects: load curriculum subjects -> subject ids -> SubjectDtos; build groupId -> subjectIds
        List<CurriculumSubjectDto> curriculumSubjects = programApi.findCurriculumSubjectsByIds(curriculumSubjectIdsSet);
        Map<UUID, CurriculumSubjectDto> curriculumSubjectById = curriculumSubjects.stream()
                .collect(Collectors.toMap(CurriculumSubjectDto::id, cs -> cs));
        Set<UUID> subjectIdsSet = curriculumSubjects.stream().map(CurriculumSubjectDto::subjectId).collect(Collectors.toSet());
        List<SubjectDto> subjectsForFilter = subjectIdsSet.isEmpty() ? List.of()
                : subjectApi.findSubjectsByIds(new ArrayList<>(subjectIdsSet)).stream()
                        .sorted(Comparator.comparing(SubjectDto::code, Comparator.nullsLast(Comparator.naturalOrder())))
                        .toList();
        Map<UUID, Set<UUID>> groupToSubjectIds = new java.util.HashMap<>();
        for (GroupSubjectOfferingDto o : offerings) {
            CurriculumSubjectDto cs = curriculumSubjectById.get(o.curriculumSubjectId());
            if (cs != null) {
                groupToSubjectIds.computeIfAbsent(o.groupId(), k -> new LinkedHashSet<>()).add(cs.subjectId());
            }
        }

        // Resolve semesters: offeringId -> dates, then date -> semester, then groupId -> semesters
        Map<UUID, Set<LocalDate>> offeringToDates = scheduleApi.findLessonDatesByOfferingIds(offeringIdsSet);
        Set<LocalDate> allDates = offeringToDates.values().stream().flatMap(Set::stream).collect(Collectors.toSet());
        List<SemesterDto> allSemestersList = academicApi.findSemestersByDates(allDates);
        Map<UUID, SemesterDto> semesterById = allSemestersList.stream().collect(Collectors.toMap(SemesterDto::id, s -> s));
        // date -> semester (first matching)
        Map<LocalDate, SemesterDto> dateToSemester = new java.util.HashMap<>();
        for (LocalDate d : allDates) {
            academicApi.findSemesterByDate(d).ifPresent(s -> dateToSemester.put(d, s));
        }
        // groupId -> set of semester IDs (from this group's offerings' lesson dates)
        Map<UUID, Set<UUID>> groupToSemesterIds = new java.util.HashMap<>();
        for (GroupSubjectOfferingDto o : offerings) {
            Set<LocalDate> dates = offeringToDates.getOrDefault(o.id(), Set.of());
            Set<UUID> semesterIds = dates.stream()
                    .map(dateToSemester::get)
                    .filter(java.util.Objects::nonNull)
                    .map(SemesterDto::id)
                    .collect(Collectors.toSet());
            groupToSemesterIds.computeIfAbsent(o.groupId(), k -> new LinkedHashSet<>()).addAll(semesterIds);
        }
        // Distinct academic years (from semesters) for filter dropdown; sort by startDate descending (newest first)
        Set<UUID> academicYearIds = allSemestersList.stream()
                .map(SemesterDto::academicYearId)
                .collect(Collectors.toSet());
        List<AcademicYearDto> academicYearsForFilter = academicApi.findAcademicYearsByIds(academicYearIds).stream()
                .sorted(Comparator.comparing(AcademicYearDto::startDate).reversed())
                .toList();
        // Semesters for filter dropdown (same as before, sorted newest first)
        List<SemesterDto> semestersForFilter = allSemestersList.stream()
                .sorted(Comparator.comparing(SemesterDto::startDate).reversed())
                .toList();

        List<StudentGroupDto> groups = groupApi.findGroupsByIds(groupIds);
        Map<UUID, StudentGroupDto> groupById = groups.stream().collect(Collectors.toMap(StudentGroupDto::id, g -> g));

        Set<UUID> programIds = new LinkedHashSet<>();
        Set<UUID> curriculumIds = new LinkedHashSet<>();
        List<UUID> curatorUserIds = new ArrayList<>();
        for (StudentGroupDto g : groups) {
            programIds.add(g.programId());
            curriculumIds.add(g.curriculumId());
            if (g.curatorUserId() != null) {
                curatorUserIds.add(g.curatorUserId());
            }
        }
        List<ProgramDto> programs = programApi.findProgramsByIds(programIds);
        List<CurriculumDto> curricula = programApi.findCurriculaByIds(curriculumIds);
        List<UserDto> users = curatorUserIds.isEmpty() ? List.of() : userApi.findByIds(curatorUserIds);

        Map<UUID, ProgramDto> programById = programs.stream().collect(Collectors.toMap(ProgramDto::id, p -> p));
        Map<UUID, CurriculumDto> curriculumById = curricula.stream().collect(Collectors.toMap(CurriculumDto::id, c -> c));
        Map<UUID, UserDto> userById = users.stream().collect(Collectors.toMap(UserDto::id, u -> u));

        Map<UUID, Long> studentCounts = studentApi.countByGroupIds(groupIds);

        List<TeacherStudentGroupItemDto> items = new ArrayList<>();
        for (UUID groupId : groupIds) {
            StudentGroupDto group = groupById.get(groupId);
            if (group == null) continue;
            ProgramDto program = programById.get(group.programId());
            CurriculumDto curriculum = curriculumById.get(group.curriculumId());
            UserDto curatorUser = group.curatorUserId() != null ? userById.get(group.curatorUserId()) : null;
            Long count = studentCounts.getOrDefault(groupId, 0L);
            Integer studentCount = count != null ? count.intValue() : null;
            Set<UUID> sidSet = groupToSemesterIds.getOrDefault(groupId, Set.of());
            List<SemesterDto> groupSemesters = sidSet.stream()
                    .map(semesterById::get)
                    .filter(java.util.Objects::nonNull)
                    .sorted(Comparator.comparing(SemesterDto::startDate).reversed())
                    .toList();
            List<UUID> groupSubjectIds = new ArrayList<>(groupToSubjectIds.getOrDefault(groupId, Set.of()));
            items.add(new TeacherStudentGroupItemDto(group, program, curriculum, curatorUser, studentCount, groupSemesters, groupSubjectIds));
        }
        return new TeacherStudentGroupsDto(academicYearsForFilter, semestersForFilter, subjectsForFilter, items);
    }
}
