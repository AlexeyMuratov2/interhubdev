package com.example.interhubdev.composition.internal;

import com.example.interhubdev.composition.LessonFullDetailsDto;
import com.example.interhubdev.composition.StudentSubjectTeacherItemDto;
import com.example.interhubdev.document.HomeworkApi;
import com.example.interhubdev.document.LessonMaterialApi;
import com.example.interhubdev.error.Errors;
import com.example.interhubdev.group.GroupApi;
import com.example.interhubdev.offering.GroupSubjectOfferingDto;
import com.example.interhubdev.offering.OfferingApi;
import com.example.interhubdev.offering.OfferingSlotDto;
import com.example.interhubdev.offering.OfferingTeacherItemDto;
import com.example.interhubdev.program.ProgramApi;
import com.example.interhubdev.schedule.RoomDto;
import com.example.interhubdev.schedule.ScheduleApi;
import com.example.interhubdev.subject.SubjectApi;
import com.example.interhubdev.teacher.TeacherApi;
import com.example.interhubdev.teacher.TeacherDto;
import com.example.interhubdev.user.UserApi;
import com.example.interhubdev.user.UserDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Use-case service: aggregates full lesson details for the "Full Lesson Information" screen.
 * Injects only the APIs needed for this composition.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
class LessonFullDetailsService {

    private final ScheduleApi scheduleApi;
    private final OfferingApi offeringApi;
    private final SubjectApi subjectApi;
    private final GroupApi groupApi;
    private final LessonMaterialApi lessonMaterialApi;
    private final HomeworkApi homeworkApi;
    private final TeacherApi teacherApi;
    private final UserApi userApi;
    private final ProgramApi programApi;

    LessonFullDetailsDto execute(UUID lessonId, UUID requesterId) {
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

        // 9. Resolve full teacher info (profile + user + role) for all offering teachers
        List<StudentSubjectTeacherItemDto> teachers = resolveTeachers(offering, offeringTeachers);

        // 10. Get offering slot (if lesson was generated from a slot)
        OfferingSlotDto offeringSlot = null;
        if (lesson.offeringSlotId() != null) {
            offeringSlot = offeringApi.findSlotsByOfferingId(offering.id()).stream()
                    .filter(s -> s.id().equals(lesson.offeringSlotId()))
                    .findFirst()
                    .orElse(null);
        }

        // 11. Get lesson materials
        var materials = lessonMaterialApi.listByLesson(lessonId, requesterId);

        // 12. Get homework assignments
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
                teachers,
                materials,
                homework
        );
    }

    /**
     * Resolve all teachers assigned to the offering (main + slot teachers) with full profile and user data.
     * Batch-loaded by teacher IDs to avoid N+1.
     */
    private List<StudentSubjectTeacherItemDto> resolveTeachers(GroupSubjectOfferingDto offering, List<OfferingTeacherItemDto> offeringTeachers) {
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
}
