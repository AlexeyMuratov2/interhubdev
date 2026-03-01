package com.example.interhubdev.attendancerecord.internal;

import com.example.interhubdev.offering.GroupSubjectOfferingDto;
import com.example.interhubdev.offering.OfferingApi;
import com.example.interhubdev.offering.OfferingTeacherItemDto;
import com.example.interhubdev.schedule.LessonDto;
import com.example.interhubdev.teacher.TeacherApi;
import com.example.interhubdev.teacher.TeacherDto;
import com.example.interhubdev.user.Role;
import com.example.interhubdev.user.UserApi;
import com.example.interhubdev.user.UserDto;

import java.util.List;
import java.util.UUID;

/**
 * Access policy for attendance record operations.
 */
final class AttendanceRecordAccessPolicy {

    private final UserApi userApi;
    private final TeacherApi teacherApi;
    private final OfferingApi offeringApi;

    AttendanceRecordAccessPolicy(UserApi userApi, TeacherApi teacherApi, OfferingApi offeringApi) {
        this.userApi = userApi;
        this.teacherApi = teacherApi;
        this.offeringApi = offeringApi;
    }

    void ensureCanManageSession(UUID userId, LessonDto lessonDto) {
        GroupSubjectOfferingDto offering = offeringApi.findOfferingById(lessonDto.offeringId())
                .orElseThrow(() -> AttendanceRecordErrors.offeringNotFound(lessonDto.offeringId()));
        ensureCanManageOffering(userId, offering);
    }

    void ensureCanManageOffering(UUID userId, GroupSubjectOfferingDto offering) {
        UserDto user = userApi.findById(userId)
                .orElseThrow(() -> AttendanceRecordErrors.forbidden("User not found"));

        if (user.hasRole(Role.ADMIN) || user.hasRole(Role.MODERATOR) || user.hasRole(Role.SUPER_ADMIN)) {
            return;
        }

        if (user.hasRole(Role.TEACHER)) {
            TeacherDto teacher = teacherApi.findByUserId(userId)
                    .orElseThrow(() -> AttendanceRecordErrors.forbidden("User does not have a teacher profile"));

            if (offering.teacherId() != null && offering.teacherId().equals(teacher.id())) {
                return;
            }

            List<OfferingTeacherItemDto> teachers = offeringApi.findTeachersByOfferingId(offering.id());
            boolean isAssignedTeacher = teachers.stream()
                    .anyMatch(t -> t.teacherId().equals(teacher.id()));
            if (!isAssignedTeacher) {
                throw AttendanceRecordErrors.forbidden("Only teachers assigned to this offering can manage attendance");
            }
            return;
        }

        throw AttendanceRecordErrors.forbidden("Only teachers or administrators can manage attendance");
    }
}
