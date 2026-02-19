package com.example.interhubdev.composition;

import com.example.interhubdev.document.HomeworkDto;
import com.example.interhubdev.document.LessonMaterialDto;
import com.example.interhubdev.group.StudentGroupDto;
import com.example.interhubdev.offering.GroupSubjectOfferingDto;
import com.example.interhubdev.offering.OfferingSlotDto;
import com.example.interhubdev.offering.OfferingTeacherItemDto;
import com.example.interhubdev.program.CurriculumSubjectDto;
import com.example.interhubdev.schedule.LessonDto;
import com.example.interhubdev.schedule.RoomDto;
import com.example.interhubdev.subject.SubjectDto;
import com.example.interhubdev.teacher.TeacherDto;

import java.util.List;

/**
 * Aggregated container for full lesson details.
 * Reuses DTOs from other modules without creating new view DTOs.
 */
public record LessonFullDetailsDto(
    /**
     * Basic lesson information (date, time, status, topic).
     */
    LessonDto lesson,

    /**
     * Subject information (name and all available subject data).
     */
    SubjectDto subject,

    /**
     * Group information (groupId and basic group info).
     */
    StudentGroupDto group,

    /**
     * Offering information (groupId, curriculumSubjectId, teacherId, etc.).
     */
    GroupSubjectOfferingDto offering,

    /**
     * Offering slot this lesson was generated from (day of week, time, lesson type).
     * Null if lesson has no offeringSlotId or slot not found.
     */
    OfferingSlotDto offeringSlot,

    /**
     * Curriculum subject information (semester, credits, hours, etc.).
     */
    CurriculumSubjectDto curriculumSubject,

    /**
     * Room information (building, room number, capacity, type).
     * Null if room is not assigned to the lesson.
     */
    RoomDto room,

    /**
     * Main teacher from offering (teacherId from offering).
     * Null if no main teacher is assigned.
     */
    TeacherDto mainTeacher,

    /**
     * All offering teachers (derived from main teacher and slot teachers; role from slot lessonType or null for main).
     * Empty list if no teachers are assigned.
     */
    List<OfferingTeacherItemDto> offeringTeachers,

    /**
     * All lesson materials linked to the lesson.
     * Empty list if no materials are attached.
     */
    List<LessonMaterialDto> materials,

    /**
     * All homework assignments linked to the lesson.
     * Empty list if no homework is assigned.
     */
    List<HomeworkDto> homework
) {
}
