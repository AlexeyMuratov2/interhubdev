package com.example.interhubdev.composition.internal;

import com.example.interhubdev.composition.StudentSubjectListItemDto;
import com.example.interhubdev.composition.StudentSubjectsDto;
import com.example.interhubdev.department.DepartmentApi;
import com.example.interhubdev.department.DepartmentDto;
import com.example.interhubdev.error.Errors;
import com.example.interhubdev.offering.GroupSubjectOfferingDto;
import com.example.interhubdev.offering.OfferingApi;
import com.example.interhubdev.offering.OfferingSlotDto;
import com.example.interhubdev.program.CurriculumSubjectDto;
import com.example.interhubdev.program.ProgramApi;
import com.example.interhubdev.schedule.ScheduleApi;
import com.example.interhubdev.student.StudentApi;
import com.example.interhubdev.subject.SubjectApi;
import com.example.interhubdev.subject.SubjectDto;
import com.example.interhubdev.teacher.TeacherApi;
import com.example.interhubdev.teacher.TeacherDto;
import com.example.interhubdev.user.UserApi;
import com.example.interhubdev.user.UserDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Use-case service: aggregates all subjects for which the current student has at least one lesson.
 * For student dashboard / subject list. Returns subject info and teacher display name per offering.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
class StudentSubjectsService {

    private final StudentApi studentApi;
    private final OfferingApi offeringApi;
    private final ScheduleApi scheduleApi;
    private final ProgramApi programApi;
    private final SubjectApi subjectApi;
    private final DepartmentApi departmentApi;
    private final TeacherApi teacherApi;
    private final UserApi userApi;

    StudentSubjectsDto execute(UUID requesterId, Optional<Integer> semesterNo) {
        if (requesterId == null) {
            throw Errors.unauthorized("Authentication required");
        }
        studentApi.findByUserId(requesterId)
                .orElseThrow(() -> Errors.forbidden("Not a student"));

        List<UUID> groupIds = studentApi.getGroupIdsByUserId(requesterId);
        if (groupIds.isEmpty()) {
            return new StudentSubjectsDto(List.of());
        }

        Set<UUID> offeringIdsSet = new LinkedHashSet<>();
        for (UUID groupId : groupIds) {
            List<GroupSubjectOfferingDto> offerings = offeringApi.findOfferingsByGroupId(groupId);
            for (GroupSubjectOfferingDto o : offerings) {
                offeringIdsSet.add(o.id());
            }
        }
        if (offeringIdsSet.isEmpty()) {
            return new StudentSubjectsDto(List.of());
        }

        List<UUID> offeringIds = new ArrayList<>(offeringIdsSet);
        Map<UUID, Set<java.time.LocalDate>> offeringToDates =
                scheduleApi.findLessonDatesByOfferingIds(offeringIds);
        Set<UUID> offeringIdsWithLessons = offeringToDates.entrySet().stream()
                .filter(e -> e.getValue() != null && !e.getValue().isEmpty())
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());
        if (offeringIdsWithLessons.isEmpty()) {
            return new StudentSubjectsDto(List.of());
        }

        List<GroupSubjectOfferingDto> offeringsWithLessons = offeringApi.findOfferingsByIds(offeringIdsWithLessons);
        Set<UUID> curriculumSubjectIdsSet = new LinkedHashSet<>();
        for (GroupSubjectOfferingDto o : offeringsWithLessons) {
            curriculumSubjectIdsSet.add(o.curriculumSubjectId());
        }
        List<CurriculumSubjectDto> curriculumSubjects =
                programApi.findCurriculumSubjectsByIds(curriculumSubjectIdsSet);
        Map<UUID, CurriculumSubjectDto> curriculumSubjectById = curriculumSubjects.stream()
                .collect(Collectors.toMap(CurriculumSubjectDto::id, cs -> cs));
        if (semesterNo.isPresent()) {
            int no = semesterNo.get();
            offeringsWithLessons = offeringsWithLessons.stream()
                    .filter(o -> {
                        CurriculumSubjectDto cs = curriculumSubjectById.get(o.curriculumSubjectId());
                        return cs != null && cs.semesterNo() == no;
                    })
                    .toList();
        }
        Set<UUID> subjectIdsSet = curriculumSubjects.stream()
                .map(CurriculumSubjectDto::subjectId)
                .collect(Collectors.toSet());
        List<SubjectDto> subjectsList = subjectIdsSet.isEmpty() ? List.of()
                : subjectApi.findSubjectsByIds(new ArrayList<>(subjectIdsSet));
        Map<UUID, SubjectDto> subjectById = subjectsList.stream()
                .collect(Collectors.toMap(SubjectDto::id, s -> s));

        List<StudentSubjectListItemDto> items = new ArrayList<>();
        for (GroupSubjectOfferingDto offering : offeringsWithLessons) {
            CurriculumSubjectDto curriculumSubject = curriculumSubjectById.get(offering.curriculumSubjectId());
            if (curriculumSubject == null) continue;
            SubjectDto subject = subjectById.get(curriculumSubject.subjectId());
            if (subject == null) continue;

            String departmentName = null;
            if (subject.departmentId() != null) {
                departmentName = departmentApi.findById(subject.departmentId())
                        .map(DepartmentDto::name)
                        .orElse(null);
            }

            UUID teacherId = offering.teacherId();
            if (teacherId == null) {
                List<OfferingSlotDto> slots = offeringApi.findSlotsByOfferingId(offering.id());
                teacherId = slots.stream()
                        .map(OfferingSlotDto::teacherId)
                        .filter(id -> id != null)
                        .findFirst()
                        .orElse(null);
            }
            String teacherDisplayName = "—";
            if (teacherId != null) {
                TeacherDto teacher = teacherApi.findById(teacherId).orElse(null);
                if (teacher != null) {
                    teacherDisplayName = userApi.findById(teacher.userId())
                            .map(UserDto::getFullName)
                            .orElse(teacher.englishName() != null ? teacher.englishName() : "—");
                }
            }

            items.add(new StudentSubjectListItemDto(
                    offering.id(),
                    curriculumSubject.id(),
                    subject.id(),
                    subject.code(),
                    subject.chineseName(),
                    subject.englishName(),
                    departmentName,
                    teacherDisplayName
            ));
        }

        items.sort(Comparator.comparing(StudentSubjectListItemDto::subjectCode, Comparator.nullsLast(Comparator.naturalOrder())));
        return new StudentSubjectsDto(items);
    }
}
