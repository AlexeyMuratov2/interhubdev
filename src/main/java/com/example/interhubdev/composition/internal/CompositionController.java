package com.example.interhubdev.composition.internal;

import com.example.interhubdev.auth.AuthApi;
import com.example.interhubdev.composition.CompositionApi;
import com.example.interhubdev.composition.GroupSubjectInfoDto;
import com.example.interhubdev.composition.LessonFullDetailsDto;
import com.example.interhubdev.composition.LessonHomeworkSubmissionsDto;
import com.example.interhubdev.composition.LessonRosterAttendanceDto;
import com.example.interhubdev.composition.StudentAttendanceHistoryDto;
import com.example.interhubdev.composition.StudentGradeHistoryDto;
import com.example.interhubdev.composition.StudentHomeworkHistoryDto;
import com.example.interhubdev.composition.StudentSubjectsDto;
import com.example.interhubdev.composition.TeacherStudentGroupsDto;
import com.example.interhubdev.error.Errors;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;
import java.util.UUID;

/**
 * REST controller for composition endpoints.
 * Thin layer: only handles HTTP, delegates to CompositionApi.
 */
@RestController
@RequestMapping("/api/composition")
@RequiredArgsConstructor
@Tag(name = "Composition", description = "Read-only data aggregation for complex UI screens")
class CompositionController {

    private final CompositionApi compositionApi;
    private final AuthApi authApi;

    /**
     * Get full details for a lesson (Use Case #1: Lesson Full Details).
     * Aggregates all data needed for the "Full Lesson Information" screen.
     *
     * @param lessonId lesson ID (path variable)
     * @param request HTTP request (for authentication)
     * @return 200 OK with LessonFullDetailsDto
     */
    @GetMapping("/lessons/{lessonId}/full-details")
    @Operation(summary = "Get full lesson details", description = "Aggregates subject, group, materials, homework, room, teachers, and offering information")
    public ResponseEntity<LessonFullDetailsDto> getLessonFullDetails(
            @PathVariable UUID lessonId,
            HttpServletRequest request
    ) {
        UUID requesterId = authApi.getCurrentUser(request)
                .map(u -> u.id())
                .orElseThrow(() -> Errors.unauthorized("Authentication required"));

        LessonFullDetailsDto details = compositionApi.getLessonFullDetails(lessonId, requesterId);
        return ResponseEntity.ok(details);
    }

    /**
     * Get roster attendance for a lesson: all students in the group with attendance status and absence notices.
     * For the lesson screen attendance table UI.
     *
     * @param lessonId       lesson (session) ID
     * @param includeCanceled if true, include CANCELED absence notices in response
     * @param request        HTTP request (for authentication)
     * @return 200 OK with LessonRosterAttendanceDto
     */
    @GetMapping("/lessons/{lessonId}/roster-attendance")
    @Operation(summary = "Get lesson roster attendance", description = "All students in the lesson's group with attendance status and absence notices for this lesson")
    public ResponseEntity<LessonRosterAttendanceDto> getLessonRosterAttendance(
            @PathVariable UUID lessonId,
            @RequestParam(defaultValue = "false") boolean includeCanceled,
            HttpServletRequest request
    ) {
        UUID requesterId = authApi.getCurrentUser(request)
                .map(u -> u.id())
                .orElseThrow(() -> Errors.unauthorized("Authentication required"));

        LessonRosterAttendanceDto dto = compositionApi.getLessonRosterAttendance(lessonId, requesterId, includeCanceled);
        return ResponseEntity.ok(dto);
    }

    /**
     * Get all homework submissions for a lesson: all students in the group with submission/points/files per homework.
     * For the lesson screen homework submissions table.
     *
     * @param lessonId lesson (session) ID
     * @param request  HTTP request (for authentication)
     * @return 200 OK with LessonHomeworkSubmissionsDto
     */
    @GetMapping("/lessons/{lessonId}/homework-submissions")
    @Operation(summary = "Get lesson homework submissions", description = "All students in the lesson's group with their submissions, points, and files per homework assignment")
    public ResponseEntity<LessonHomeworkSubmissionsDto> getLessonHomeworkSubmissions(
            @PathVariable UUID lessonId,
            HttpServletRequest request
    ) {
        UUID requesterId = authApi.getCurrentUser(request)
                .map(u -> u.id())
                .orElseThrow(() -> Errors.unauthorized("Authentication required"));

        LessonHomeworkSubmissionsDto dto = compositionApi.getLessonHomeworkSubmissions(lessonId, requesterId);
        return ResponseEntity.ok(dto);
    }

    /**
     * Get student groups where the current teacher has at least one lesson.
     * For teacher dashboard "Student groups" page.
     *
     * @param request HTTP request (for authentication)
     * @return 200 OK with TeacherStudentGroupsDto
     */
    @GetMapping("/teacher/student-groups")
    @Operation(summary = "Get teacher student groups", description = "Groups where the teacher has at least one lesson (slots with lessons only)")
    public ResponseEntity<TeacherStudentGroupsDto> getTeacherStudentGroups(HttpServletRequest request) {
        UUID requesterId = authApi.getCurrentUser(request)
                .map(u -> u.id())
                .orElseThrow(() -> Errors.unauthorized("Authentication required"));

        TeacherStudentGroupsDto dto = compositionApi.getTeacherStudentGroups(requesterId);
        return ResponseEntity.ok(dto);
    }

    /**
     * Get all subjects for which the current student has at least one lesson.
     * For student dashboard / subject list.
     *
     * @param request HTTP request (for authentication)
     * @return 200 OK with StudentSubjectsDto
     */
    @GetMapping("/student/subjects")
    @Operation(summary = "Get student subjects", description = "All subjects for which the student has at least one lesson")
    public ResponseEntity<StudentSubjectsDto> getStudentSubjects(HttpServletRequest request) {
        UUID requesterId = authApi.getCurrentUser(request)
                .map(u -> u.id())
                .orElseThrow(() -> Errors.unauthorized("Authentication required"));

        StudentSubjectsDto dto = compositionApi.getStudentSubjects(requesterId);
        return ResponseEntity.ok(dto);
    }

    /**
     * Get full info for a group and subject (teacher's "Group subject info" screen).
     * Only teachers assigned to an offering slot for this subject and group (or admin) can view.
     *
     * @param groupId   group ID (path)
     * @param subjectId subject ID (path)
     * @param semesterId optional semester; if absent, current semester is used
     * @param request   HTTP request (for authentication)
     * @return 200 OK with GroupSubjectInfoDto
     */
    @GetMapping("/groups/{groupId}/subjects/{subjectId}/info")
    @Operation(summary = "Get group subject info", description = "Full info for group and subject: offering, slots, curriculum, students with points, homework submissions, attendance. Only for teacher of this offering or admin.")
    public ResponseEntity<GroupSubjectInfoDto> getGroupSubjectInfo(
            @PathVariable UUID groupId,
            @PathVariable UUID subjectId,
            @RequestParam(required = false) UUID semesterId,
            HttpServletRequest request
    ) {
        UUID requesterId = authApi.getCurrentUser(request)
                .map(u -> u.id())
                .orElseThrow(() -> Errors.unauthorized("Authentication required"));

        GroupSubjectInfoDto dto = compositionApi.getGroupSubjectInfo(
                groupId, subjectId, requesterId, Optional.ofNullable(semesterId));
        return ResponseEntity.ok(dto);
    }

    /**
     * Get full grade history for a student in an offering.
     * All grade entries with lesson, homework, submission and grader context for display.
     *
     * @param studentId  student profile ID (path)
     * @param offeringId offering ID (path)
     * @param request    HTTP request (for authentication)
     * @return 200 OK with StudentGradeHistoryDto
     */
    @GetMapping("/students/{studentId}/offerings/{offeringId}/grade-history")
    @Operation(summary = "Get student grade history", description = "All grades for a student in an offering with lesson, homework, submission and grader context")
    public ResponseEntity<StudentGradeHistoryDto> getStudentGradeHistory(
            @PathVariable UUID studentId,
            @PathVariable UUID offeringId,
            HttpServletRequest request
    ) {
        UUID requesterId = authApi.getCurrentUser(request)
                .map(u -> u.id())
                .orElseThrow(() -> Errors.unauthorized("Authentication required"));

        StudentGradeHistoryDto dto = compositionApi.getStudentGradeHistory(studentId, offeringId, requesterId);
        return ResponseEntity.ok(dto);
    }

    /**
     * Get attendance history for a student in an offering.
     * All lessons for the offering with attendance and absence notices per lesson; summary counts for UI.
     *
     * @param studentId  student profile ID (path)
     * @param offeringId offering ID (path)
     * @param request    HTTP request (for authentication)
     * @return 200 OK with StudentAttendanceHistoryDto
     */
    @GetMapping("/students/{studentId}/offerings/{offeringId}/attendance-history")
    @Operation(summary = "Get student attendance history", description = "All lessons for the offering with student attendance and absence notices per lesson; missed count and notices count")
    public ResponseEntity<StudentAttendanceHistoryDto> getStudentAttendanceHistory(
            @PathVariable UUID studentId,
            @PathVariable UUID offeringId,
            HttpServletRequest request
    ) {
        UUID requesterId = authApi.getCurrentUser(request)
                .map(u -> u.id())
                .orElseThrow(() -> Errors.unauthorized("Authentication required"));

        StudentAttendanceHistoryDto dto = compositionApi.getStudentAttendanceHistory(studentId, offeringId, requesterId);
        return ResponseEntity.ok(dto);
    }

    /**
     * Get full homework history for a student in an offering.
     * All homework assignments for the offering and the student's submission and grade per assignment.
     *
     * @param studentId  student profile ID (path)
     * @param offeringId offering ID (path)
     * @param request    HTTP request (for authentication)
     * @return 200 OK with StudentHomeworkHistoryDto
     */
    @GetMapping("/students/{studentId}/offerings/{offeringId}/homework-history")
    @Operation(summary = "Get student homework history", description = "All homeworks for the offering with student's submission and grade per assignment; full data for frontend student view")
    public ResponseEntity<StudentHomeworkHistoryDto> getStudentHomeworkHistory(
            @PathVariable UUID studentId,
            @PathVariable UUID offeringId,
            HttpServletRequest request
    ) {
        UUID requesterId = authApi.getCurrentUser(request)
                .map(u -> u.id())
                .orElseThrow(() -> Errors.unauthorized("Authentication required"));

        StudentHomeworkHistoryDto dto = compositionApi.getStudentHomeworkHistory(studentId, offeringId, requesterId);
        return ResponseEntity.ok(dto);
    }
}
