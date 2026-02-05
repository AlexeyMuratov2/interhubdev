package com.example.interhubdev.group.internal;

import com.example.interhubdev.group.GroupCurriculumOverrideDto;
import com.example.interhubdev.group.GroupLeaderDto;
import com.example.interhubdev.group.StudentGroupDto;

final class GroupMappers {

    private GroupMappers() {
    }

    static StudentGroupDto toGroupDto(StudentGroup e) {
        return new StudentGroupDto(e.getId(), e.getProgramId(), e.getCurriculumId(), e.getCode(), e.getName(),
                e.getDescription(), e.getStartYear(), e.getGraduationYear(), e.getCuratorUserId(),
                e.getCreatedAt(), e.getUpdatedAt());
    }

    static GroupLeaderDto toLeaderDto(GroupLeader e) {
        return new GroupLeaderDto(e.getId(), e.getGroupId(), e.getStudentId(), e.getRole(),
                e.getFromDate(), e.getToDate(), e.getCreatedAt());
    }

    static GroupCurriculumOverrideDto toOverrideDto(GroupCurriculumOverride e) {
        return new GroupCurriculumOverrideDto(e.getId(), e.getGroupId(), e.getCurriculumSubjectId(), e.getSubjectId(),
                e.getAction(), e.getNewAssessmentTypeId(), e.getNewDurationWeeks(), e.getReason(), e.getCreatedAt());
    }
}

