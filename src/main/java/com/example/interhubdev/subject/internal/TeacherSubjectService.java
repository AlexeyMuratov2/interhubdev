package com.example.interhubdev.subject.internal;

import com.example.interhubdev.department.DepartmentApi;
import com.example.interhubdev.department.DepartmentDto;
import com.example.interhubdev.document.CourseMaterialApi;
import com.example.interhubdev.document.CourseMaterialDto;
import com.example.interhubdev.group.GroupApi;
import com.example.interhubdev.group.StudentGroupDto;
import com.example.interhubdev.program.CurriculumSubjectAssessmentDto;
import com.example.interhubdev.program.CurriculumSubjectDto;
import com.example.interhubdev.schedule.RoomDto;
import com.example.interhubdev.schedule.ScheduleApi;
import com.example.interhubdev.subject.*;
import com.example.interhubdev.user.UserApi;
import com.example.interhubdev.user.UserDto;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Internal service for teacher subject operations.
 * Aggregates data from multiple modules to build teacher subject responses.
 */
@Service
@Transactional(readOnly = true)
class TeacherSubjectService {

    private final OfferingLookupPort offeringLookupPort;
    private final CurriculumSubjectLookupPort curriculumSubjectLookupPort;
    private final SubjectApi subjectApi;
    private final GroupApi groupApi;
    private final DepartmentApi departmentApi;
    private final ScheduleApi scheduleApi;
    private final CourseMaterialApi courseMaterialApi;
    private final UserApi userApi;

    public TeacherSubjectService(
            OfferingLookupPort offeringLookupPort,
            CurriculumSubjectLookupPort curriculumSubjectLookupPort,
            @Lazy SubjectApi subjectApi,
            @Lazy GroupApi groupApi,
            DepartmentApi departmentApi,
            ScheduleApi scheduleApi,
            CourseMaterialApi courseMaterialApi,
            UserApi userApi
    ) {
        this.offeringLookupPort = offeringLookupPort;
        this.curriculumSubjectLookupPort = curriculumSubjectLookupPort;
        this.subjectApi = subjectApi;
        this.groupApi = groupApi;
        this.departmentApi = departmentApi;
        this.scheduleApi = scheduleApi;
        this.courseMaterialApi = courseMaterialApi;
        this.userApi = userApi;
    }

    /**
     * Get list of teacher subjects (shortened view) filtered by semester.
     *
     * @param teacherId teacher entity ID
     * @param semesterNo optional semester number filter (1 or 2)
     * @return list of teacher subject items
     */
    List<TeacherSubjectListItemDto> findTeacherSubjects(UUID teacherId, Integer semesterNo) {
        // Get all offerings for teacher
        List<GroupSubjectOfferingDto> offerings = offeringLookupPort.findOfferingsByTeacherId(teacherId);
        if (offerings.isEmpty()) {
            return List.of();
        }

        // Group offerings by curriculumSubjectId
        Map<UUID, List<GroupSubjectOfferingDto>> offeringsByCurriculumSubject = offerings.stream()
                .collect(Collectors.groupingBy(GroupSubjectOfferingDto::curriculumSubjectId));

        // Get all curriculum subjects
        List<UUID> curriculumSubjectIds = new ArrayList<>(offeringsByCurriculumSubject.keySet());
        Map<UUID, CurriculumSubjectDto> curriculumSubjectsById = curriculumSubjectIds.stream()
                .map(curriculumSubjectLookupPort::findById)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .filter(cs -> semesterNo == null || cs.semesterNo() == semesterNo)
                .collect(Collectors.toMap(CurriculumSubjectDto::id, cs -> cs));

        if (curriculumSubjectsById.isEmpty()) {
            return List.of();
        }

        // Get all subjects
        List<UUID> subjectIds = curriculumSubjectsById.values().stream()
                .map(CurriculumSubjectDto::subjectId)
                .distinct()
                .toList();
        Map<UUID, SubjectDto> subjectsById = subjectApi.findSubjectsByIds(subjectIds).stream()
                .collect(Collectors.toMap(SubjectDto::id, s -> s));

        // Get all assessment types
        Set<UUID> assessmentTypeIds = curriculumSubjectsById.values().stream()
                .map(CurriculumSubjectDto::assessmentTypeId)
                .collect(Collectors.toSet());
        Map<UUID, AssessmentTypeDto> assessmentTypesById = assessmentTypeIds.stream()
                .map(subjectApi::findAssessmentTypeById)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toMap(AssessmentTypeDto::id, at -> at));

        // Get all departments
        Set<UUID> departmentIds = subjectsById.values().stream()
                .map(SubjectDto::departmentId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<UUID, DepartmentDto> departmentsById = departmentIds.stream()
                .map(departmentApi::findById)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toMap(DepartmentDto::id, d -> d));

        // Get all groups
        Set<UUID> groupIds = offerings.stream()
                .map(GroupSubjectOfferingDto::groupId)
                .collect(Collectors.toSet());
        Map<UUID, StudentGroupDto> groupsById = groupIds.stream()
                .map(groupApi::findGroupById)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toMap(StudentGroupDto::id, g -> g));

        // Build response
        return curriculumSubjectsById.values().stream()
                .sorted(Comparator.comparing(CurriculumSubjectDto::semesterNo)
                        .thenComparing(cs -> subjectsById.getOrDefault(cs.subjectId(), null),
                                Comparator.nullsLast(Comparator.comparing(SubjectDto::code))))
                .map(cs -> {
                    SubjectDto subject = subjectsById.get(cs.subjectId());
                    AssessmentTypeDto assessmentType = assessmentTypesById.get(cs.assessmentTypeId());
                    DepartmentDto department = cs.subjectId() != null && subject != null && subject.departmentId() != null
                            ? departmentsById.get(subject.departmentId()) : null;
                    List<StudentGroupDto> groups = offeringsByCurriculumSubject.get(cs.id()).stream()
                            .map(offering -> groupsById.get(offering.groupId()))
                            .filter(Objects::nonNull)
                            .collect(Collectors.toList());
                    return TeacherSubjectMappers.toListItemDto(cs, subject, assessmentType, department, groups);
                })
                .toList();
    }

    /**
     * Get full detail of a teacher subject.
     *
     * @param curriculumSubjectId curriculum subject ID
     * @param teacherId teacher entity ID
     * @param requesterId current authenticated user ID (for access control)
     * @return teacher subject detail DTO
     */
    TeacherSubjectDetailDto findTeacherSubjectDetail(UUID curriculumSubjectId, UUID teacherId, UUID requesterId) {
        // Verify curriculum subject exists
        CurriculumSubjectDto curriculumSubject = curriculumSubjectLookupPort.findById(curriculumSubjectId)
                .orElseThrow(() -> SubjectErrors.curriculumSubjectNotFound(curriculumSubjectId));

        // Verify teacher has access to this curriculum subject
        List<GroupSubjectOfferingDto> teacherOfferings = offeringLookupPort
                .findOfferingsByCurriculumSubjectIdAndTeacherId(curriculumSubjectId, teacherId);
        if (teacherOfferings.isEmpty()) {
            throw SubjectErrors.accessDenied();
        }

        // Get subject
        SubjectDto subject = subjectApi.findSubjectById(curriculumSubject.subjectId())
                .orElseThrow(() -> SubjectErrors.curriculumSubjectNotFound(curriculumSubjectId));

        // Get department
        DepartmentDto department = subject.departmentId() != null
                ? departmentApi.findById(subject.departmentId()).orElse(null)
                : null;

        // Get assessments
        List<CurriculumSubjectAssessmentDto> assessments = curriculumSubjectLookupPort
                .findAssessmentsByCurriculumSubjectId(curriculumSubjectId);
        Set<UUID> assessmentTypeIds = assessments.stream()
                .map(CurriculumSubjectAssessmentDto::assessmentTypeId)
                .collect(Collectors.toSet());
        Map<UUID, AssessmentTypeDto> assessmentTypesById = assessmentTypeIds.stream()
                .map(subjectApi::findAssessmentTypeById)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toMap(AssessmentTypeDto::id, at -> at));

        // Get groups and rooms
        Set<UUID> groupIds = teacherOfferings.stream()
                .map(GroupSubjectOfferingDto::groupId)
                .collect(Collectors.toSet());
        Map<UUID, StudentGroupDto> groupsById = groupIds.stream()
                .map(groupApi::findGroupById)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toMap(StudentGroupDto::id, g -> g));

        Set<UUID> roomIds = teacherOfferings.stream()
                .map(GroupSubjectOfferingDto::roomId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<UUID, RoomDto> roomsById = roomIds.stream()
                .map(scheduleApi::findRoomById)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toMap(RoomDto::id, r -> r));

        // Get materials for each offering
        Set<UUID> authorIds = new HashSet<>();
        Map<UUID, List<CourseMaterialDto>> materialsByOfferingId = new HashMap<>();
        for (GroupSubjectOfferingDto offering : teacherOfferings) {
            List<CourseMaterialDto> materials = courseMaterialApi.listByOffering(offering.id(), requesterId);
            materialsByOfferingId.put(offering.id(), materials);
            materials.forEach(m -> authorIds.add(m.authorId()));
        }

        // Get users (authors)
        Map<UUID, UserDto> usersById = userApi.findByIds(authorIds).stream()
                .collect(Collectors.toMap(UserDto::id, u -> u));

        // Get assessment type for curriculum subject
        AssessmentTypeDto curriculumSubjectAssessmentType = subjectApi
                .findAssessmentTypeById(curriculumSubject.assessmentTypeId())
                .orElse(null);

        // Build response
        return new TeacherSubjectDetailDto(
            TeacherSubjectMappers.toSubjectInfoDto(subject, department),
            TeacherSubjectMappers.toCurriculumSubjectInfoDto(curriculumSubject, curriculumSubjectAssessmentType),
            assessments.stream()
                    .map(a -> TeacherSubjectMappers.toAssessmentInfoDto(a, assessmentTypesById.get(a.assessmentTypeId())))
                    .toList(),
            teacherOfferings.stream()
                    .map(offering -> TeacherSubjectMappers.toOfferingInfoDto(
                            offering,
                            groupsById.get(offering.groupId()),
                            offering.roomId() != null ? roomsById.get(offering.roomId()) : null,
                            materialsByOfferingId.getOrDefault(offering.id(), List.of()),
                            usersById))
                    .toList()
        );
    }
}
