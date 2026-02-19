package com.example.interhubdev.attendance.internal;

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
 * Access policy for attendance operations.
 * Provides methods to check if a user can perform attendance-related actions.
 */
final class AttendanceAccessPolicy {

    private final UserApi userApi;
    private final TeacherApi teacherApi;
    private final OfferingApi offeringApi;

    AttendanceAccessPolicy(UserApi userApi, TeacherApi teacherApi, OfferingApi offeringApi) {
        this.userApi = userApi;
        this.teacherApi = teacherApi;
        this.offeringApi = offeringApi;
    }

    /**
     * Ensure user can manage (read/write) attendance for a lesson session.
     * User must be teacher of the session's offering or admin/moderator.
     *
     * @param userId    user ID
     * @param lessonDto lesson DTO (must have offeringId)
     * @throws com.example.interhubdev.error.AppException FORBIDDEN if user cannot manage session
     */
    void ensureCanManageSession(UUID userId, LessonDto lessonDto) {
        GroupSubjectOfferingDto offering = offeringApi.findOfferingById(lessonDto.offeringId())
                .orElseThrow(() -> AttendanceErrors.offeringNotFound(lessonDto.offeringId()));
        ensureCanManageOffering(userId, offering);
    }

    /**
     * Ensure user can manage (read/write) attendance for an offering.
     * User must be teacher of the offering or admin/moderator.
     *
     * @param userId   user ID
     * @param offering offering DTO
     * @throws com.example.interhubdev.error.AppException FORBIDDEN if user cannot manage offering
     */
    void ensureCanManageOffering(UUID userId, GroupSubjectOfferingDto offering) {
        UserDto user = userApi.findById(userId)
                .orElseThrow(() -> AttendanceErrors.forbidden("User not found"));

        // Admin/mod/moderator can always manage
        if (user.hasRole(Role.ADMIN) || user.hasRole(Role.MODERATOR) || user.hasRole(Role.SUPER_ADMIN)) {
            return;
        }

        // Teacher must be assigned to this offering
        if (user.hasRole(Role.TEACHER)) {
            TeacherDto teacher = teacherApi.findByUserId(userId)
                    .orElseThrow(() -> AttendanceErrors.forbidden("User does not have a teacher profile"));

            // Check if teacher is main teacher of offering
            if (offering.teacherId() != null && offering.teacherId().equals(teacher.id())) {
                return;
            }

            // Check if teacher is assigned as offering teacher
            List<OfferingTeacherItemDto> teachers = offeringApi.findTeachersByOfferingId(offering.id());
            boolean isAssignedTeacher = teachers.stream()
                    .anyMatch(t -> t.teacherId().equals(teacher.id()));
            if (!isAssignedTeacher) {
                throw AttendanceErrors.forbidden("Only teachers assigned to this offering can manage attendance");
            }
            return;
        }

        throw AttendanceErrors.forbidden("Only teachers or administrators can manage attendance");
    }

    /**
     * Ensure student can only access their own notices.
     *
     * @param studentId   student profile ID from notice
     * @param requesterId user ID of requester
     * @throws com.example.interhubdev.error.AppException FORBIDDEN if requester is not the student
     */
    void ensureStudentOwnsNotice(UUID studentId, UUID requesterId) {
        // For now, we'll check this in use-case by comparing studentId from notice with studentId from requester's profile
        // This method is a placeholder for future validation if needed
    }
}
