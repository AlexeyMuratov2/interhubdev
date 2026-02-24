package com.example.interhubdev.composition.internal;

import com.example.interhubdev.composition.LessonFullDetailsDto;
import com.example.interhubdev.document.HomeworkApi;
import com.example.interhubdev.document.LessonMaterialApi;
import com.example.interhubdev.error.Errors;
import com.example.interhubdev.group.GroupApi;
import com.example.interhubdev.offering.OfferingApi;
import com.example.interhubdev.offering.OfferingSlotDto;
import com.example.interhubdev.program.ProgramApi;
import com.example.interhubdev.schedule.RoomDto;
import com.example.interhubdev.schedule.ScheduleApi;
import com.example.interhubdev.subject.SubjectApi;
import com.example.interhubdev.teacher.TeacherApi;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

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

        // 9. Get offering slot (if lesson was generated from a slot)
        OfferingSlotDto offeringSlot = null;
        if (lesson.offeringSlotId() != null) {
            offeringSlot = offeringApi.findSlotsByOfferingId(offering.id()).stream()
                    .filter(s -> s.id().equals(lesson.offeringSlotId()))
                    .findFirst()
                    .orElse(null);
        }

        // 10. Get lesson materials
        var materials = lessonMaterialApi.listByLesson(lessonId, requesterId);

        // 11. Get homework assignments
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
                materials,
                homework
        );
    }
}
