package com.example.interhubdev.offering.internal;

import com.example.interhubdev.offering.GroupSubjectOfferingDto;
import com.example.interhubdev.offering.OfferingTeacherDto;

import java.time.LocalDateTime;

final class OfferingMappers {

    private OfferingMappers() {
    }

    static GroupSubjectOfferingDto toOfferingDto(GroupSubjectOffering e) {
        return new GroupSubjectOfferingDto(
                e.getId(),
                e.getGroupId(),
                e.getCurriculumSubjectId(),
                e.getTeacherId(),
                e.getRoomId(),
                e.getFormat(),
                e.getNotes(),
                e.getCreatedAt(),
                e.getUpdatedAt()
        );
    }

    static OfferingTeacherDto toTeacherDto(OfferingTeacher e) {
        return new OfferingTeacherDto(
                e.getId(),
                e.getOfferingId(),
                e.getTeacherId(),
                e.getRole(),
                e.getCreatedAt()
        );
    }
}
