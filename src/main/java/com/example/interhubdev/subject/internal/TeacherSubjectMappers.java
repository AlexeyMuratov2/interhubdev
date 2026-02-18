package com.example.interhubdev.subject.internal;

import com.example.interhubdev.department.DepartmentDto;
import com.example.interhubdev.document.CourseMaterialDto;
import com.example.interhubdev.document.StoredFileDto;
import com.example.interhubdev.group.StudentGroupDto;
import com.example.interhubdev.program.CurriculumSubjectAssessmentDto;
import com.example.interhubdev.program.CurriculumSubjectDto;
import com.example.interhubdev.schedule.RoomDto;
import com.example.interhubdev.subject.*;
import com.example.interhubdev.user.UserDto;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Mappers for teacher subject DTOs.
 * Converts data from various modules into teacher subject response DTOs.
 */
final class TeacherSubjectMappers {

    private TeacherSubjectMappers() {
    }

    static TeacherSubjectListItemDto toListItemDto(
            CurriculumSubjectDto curriculumSubject,
            SubjectDto subject,
            AssessmentTypeDto assessmentType,
            DepartmentDto department,
            List<StudentGroupDto> groups) {
        return new TeacherSubjectListItemDto(
            curriculumSubject.id(),
            curriculumSubject.subjectId(),
            subject.code(),
            subject.chineseName(),
            subject.englishName(),
            subject.description(),
            subject.departmentId(),
            department != null ? department.name() : null,
            curriculumSubject.semesterNo(),
            curriculumSubject.courseYear(),
            curriculumSubject.durationWeeks(),
            curriculumSubject.assessmentTypeId(),
            assessmentType != null ? getAssessmentTypeName(assessmentType) : null,
            curriculumSubject.credits(),
            groups.stream()
                    .map(g -> new TeacherSubjectListItemDto.GroupInfoDto(g.id(), g.code(), g.name()))
                    .collect(Collectors.toList())
        );
    }

    static TeacherSubjectDetailDto.SubjectInfoDto toSubjectInfoDto(
            SubjectDto subject,
            DepartmentDto department) {
        return new TeacherSubjectDetailDto.SubjectInfoDto(
            subject.id(),
            subject.code(),
            subject.chineseName(),
            subject.englishName(),
            subject.description(),
            subject.departmentId(),
            department != null ? department.name() : null,
            subject.createdAt(),
            subject.updatedAt()
        );
    }

    static TeacherSubjectDetailDto.CurriculumSubjectInfoDto toCurriculumSubjectInfoDto(
            CurriculumSubjectDto curriculumSubject,
            AssessmentTypeDto assessmentType) {
        return new TeacherSubjectDetailDto.CurriculumSubjectInfoDto(
            curriculumSubject.id(),
            curriculumSubject.curriculumId(),
            curriculumSubject.subjectId(),
            curriculumSubject.semesterNo(),
            curriculumSubject.courseYear(),
            curriculumSubject.durationWeeks(),
            curriculumSubject.hoursTotal(),
            curriculumSubject.hoursLecture(),
            curriculumSubject.hoursPractice(),
            curriculumSubject.hoursLab(),
            curriculumSubject.hoursSeminar(),
            curriculumSubject.hoursSelfStudy(),
            curriculumSubject.hoursConsultation(),
            curriculumSubject.hoursCourseWork(),
            curriculumSubject.assessmentTypeId(),
            assessmentType != null ? getAssessmentTypeName(assessmentType) : null,
            curriculumSubject.credits(),
            curriculumSubject.createdAt(),
            curriculumSubject.updatedAt()
        );
    }

    static TeacherSubjectDetailDto.CurriculumSubjectAssessmentInfoDto toAssessmentInfoDto(
            CurriculumSubjectAssessmentDto assessment,
            AssessmentTypeDto assessmentType) {
        return new TeacherSubjectDetailDto.CurriculumSubjectAssessmentInfoDto(
            assessment.id(),
            assessment.assessmentTypeId(),
            assessmentType != null ? getAssessmentTypeName(assessmentType) : null,
            assessment.weekNumber(),
            assessment.isFinal(),
            assessment.weight(),
            assessment.notes(),
            assessment.createdAt()
        );
    }

    static TeacherSubjectDetailDto.GroupSubjectOfferingInfoDto toOfferingInfoDto(
            GroupSubjectOfferingDto offering,
            StudentGroupDto group,
            RoomDto room,
            List<CourseMaterialDto> materials,
            Map<UUID, UserDto> usersById) {
        return new TeacherSubjectDetailDto.GroupSubjectOfferingInfoDto(
            offering.id(),
            offering.groupId(),
            group != null ? group.code() : null,
            group != null ? group.name() : null,
            offering.teacherId(),
            offering.roomId(),
            room != null ? (room.buildingName() + " " + room.number()) : null,
            offering.format(),
            offering.notes(),
            offering.createdAt(),
            offering.updatedAt(),
            materials.stream()
                    .map(m -> toCourseMaterialInfoDto(m, usersById.get(m.authorId())))
                    .collect(Collectors.toList())
        );
    }

    static TeacherSubjectDetailDto.CourseMaterialInfoDto toCourseMaterialInfoDto(
            CourseMaterialDto material,
            UserDto author) {
        return new TeacherSubjectDetailDto.CourseMaterialInfoDto(
            material.id(),
            material.title(),
            material.description(),
            material.authorId(),
            author != null ? getUserDisplayName(author) : null,
            material.uploadedAt(),
            toStoredFileInfoDto(material.file())
        );
    }

    static TeacherSubjectDetailDto.StoredFileInfoDto toStoredFileInfoDto(StoredFileDto file) {
        return new TeacherSubjectDetailDto.StoredFileInfoDto(
            file.id(),
            file.originalName(),
            file.contentType(),
            file.size(),
            file.uploadedAt(),
            file.uploadedBy()
        );
    }

    private static String getAssessmentTypeName(AssessmentTypeDto assessmentType) {
        if (assessmentType.englishName() != null && !assessmentType.englishName().isBlank()) {
            return assessmentType.englishName();
        }
        if (assessmentType.chineseName() != null && !assessmentType.chineseName().isBlank()) {
            return assessmentType.chineseName();
        }
        return assessmentType.code() != null ? assessmentType.code() : "";
    }

    private static String getUserDisplayName(UserDto user) {
        if (user.firstName() != null && user.lastName() != null) {
            return user.firstName() + " " + user.lastName();
        }
        if (user.firstName() != null) {
            return user.firstName();
        }
        if (user.lastName() != null) {
            return user.lastName();
        }
        return user.email();
    }
}
