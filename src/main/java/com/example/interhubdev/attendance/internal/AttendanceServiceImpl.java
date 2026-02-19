package com.example.interhubdev.attendance.internal;

import com.example.interhubdev.attendance.*;
import com.example.interhubdev.auth.AuthApi;
import com.example.interhubdev.group.GroupApi;
import com.example.interhubdev.offering.GroupSubjectOfferingDto;
import com.example.interhubdev.offering.OfferingApi;
import com.example.interhubdev.offering.OfferingTeacherDto;
import com.example.interhubdev.schedule.LessonDto;
import com.example.interhubdev.schedule.ScheduleApi;
import com.example.interhubdev.student.StudentApi;
import com.example.interhubdev.student.StudentDto;
import com.example.interhubdev.teacher.TeacherApi;
import com.example.interhubdev.teacher.TeacherDto;
import com.example.interhubdev.user.Role;
import com.example.interhubdev.user.UserApi;
import com.example.interhubdev.user.UserDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Implementation of {@link AttendanceApi}: mark and query attendance records.
 */
@Service
@RequiredArgsConstructor
class AttendanceServiceImpl implements AttendanceApi {

    private final AttendanceRecordRepository repository;
    private final ScheduleApi scheduleApi;
    private final OfferingApi offeringApi;
    private final StudentApi studentApi;
    private final GroupApi groupApi;
    private final TeacherApi teacherApi;
    private final UserApi userApi;

    @Override
    @Transactional
    public List<AttendanceRecordDto> markAttendanceBulk(UUID sessionId, List<MarkAttendanceItem> items, UUID markedBy) {
        if (items == null || items.isEmpty()) {
            return List.of();
        }

        // Get lesson and validate
        LessonDto lesson = scheduleApi.findLessonById(sessionId)
                .orElseThrow(() -> AttendanceErrors.lessonNotFound(sessionId));

        // Get offering to get groupId and teacherIds
        GroupSubjectOfferingDto offering = offeringApi.findOfferingById(lesson.offeringId())
                .orElseThrow(() -> AttendanceErrors.offeringNotFound(lesson.offeringId()));

        // Check authorization: teacher must be assigned to this offering
        ensureCanMarkAttendance(markedBy, offering);

        // Get roster of students in the group
        List<StudentDto> roster = studentApi.findByGroupId(offering.groupId());
        Set<UUID> rosterStudentIds = roster.stream()
                .map(StudentDto::id)
                .collect(Collectors.toSet());

        // Validate and upsert each item
        LocalDateTime now = LocalDateTime.now();
        List<AttendanceRecord> toSave = new ArrayList<>();

        for (MarkAttendanceItem item : items) {
            // Validate student exists and is in roster
            if (!rosterStudentIds.contains(item.studentId())) {
                throw AttendanceErrors.studentNotInGroup(item.studentId(), offering.groupId());
            }
            studentApi.findById(item.studentId())
                    .orElseThrow(() -> AttendanceErrors.studentNotFound(item.studentId()));

            // Validate status and minutesLate
            AttendanceValidation.validateStatusAndMinutesLate(item.status(), item.minutesLate());
            AttendanceValidation.validateTeacherComment(item.teacherComment());

            // Find existing record or create new
            Optional<AttendanceRecord> existing = repository.findByLessonSessionIdAndStudentId(sessionId, item.studentId());
            AttendanceRecord record;
            if (existing.isPresent()) {
                record = existing.get();
                record.setStatus(item.status());
                record.setMinutesLate(item.minutesLate());
                record.setTeacherComment(item.teacherComment());
                record.setMarkedBy(markedBy);
                record.setMarkedAt(now);
            } else {
                record = AttendanceRecord.builder()
                        .lessonSessionId(sessionId)
                        .studentId(item.studentId())
                        .status(item.status())
                        .minutesLate(item.minutesLate())
                        .teacherComment(item.teacherComment())
                        .markedBy(markedBy)
                        .markedAt(now)
                        .build();
            }
            toSave.add(record);
        }

        // Save all in transaction (all-or-nothing)
        List<AttendanceRecord> saved = repository.saveAll(toSave);

        // TODO: publish IntegrationEvent AttendanceMarked for each record
        // IntegrationEvent event = new AttendanceMarkedEvent(recordId, sessionId, studentId, status, markedBy, markedAt);
        // eventPublisher.publish(event);

        return saved.stream()
                .map(AttendanceMappers::toDto)
                .toList();
    }

    @Override
    @Transactional
    public AttendanceRecordDto markAttendanceSingle(
            UUID sessionId,
            UUID studentId,
            AttendanceStatus status,
            Integer minutesLate,
            String teacherComment,
            UUID markedBy
    ) {
        // Get lesson and validate
        LessonDto lesson = scheduleApi.findLessonById(sessionId)
                .orElseThrow(() -> AttendanceErrors.lessonNotFound(sessionId));

        // Get offering to get groupId and teacherIds
        GroupSubjectOfferingDto offering = offeringApi.findOfferingById(lesson.offeringId())
                .orElseThrow(() -> AttendanceErrors.offeringNotFound(lesson.offeringId()));

        // Check authorization
        ensureCanMarkAttendance(markedBy, offering);

        // Validate student exists and is in roster
        List<StudentDto> roster = studentApi.findByGroupId(offering.groupId());
        boolean studentInRoster = roster.stream()
                .anyMatch(s -> s.id().equals(studentId));
        if (!studentInRoster) {
            throw AttendanceErrors.studentNotInGroup(studentId, offering.groupId());
        }
        studentApi.findById(studentId)
                .orElseThrow(() -> AttendanceErrors.studentNotFound(studentId));

        // Validate status and minutesLate
        AttendanceValidation.validateStatusAndMinutesLate(status, minutesLate);
        AttendanceValidation.validateTeacherComment(teacherComment);

        // Find existing or create new
        Optional<AttendanceRecord> existing = repository.findByLessonSessionIdAndStudentId(sessionId, studentId);
        LocalDateTime now = LocalDateTime.now();
        AttendanceRecord record;
        if (existing.isPresent()) {
            record = existing.get();
            record.setStatus(status);
            record.setMinutesLate(minutesLate);
            record.setTeacherComment(teacherComment);
            record.setMarkedBy(markedBy);
            record.setMarkedAt(now);
        } else {
            record = AttendanceRecord.builder()
                    .lessonSessionId(sessionId)
                    .studentId(studentId)
                    .status(status)
                    .minutesLate(minutesLate)
                    .teacherComment(teacherComment)
                    .markedBy(markedBy)
                    .markedAt(now)
                    .build();
        }
        AttendanceRecord saved = repository.save(record);

        // TODO: publish IntegrationEvent AttendanceMarked
        // IntegrationEvent event = new AttendanceMarkedEvent(saved.getId(), sessionId, studentId, status, markedBy, now);
        // eventPublisher.publish(event);

        return AttendanceMappers.toDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public SessionAttendanceDto getSessionAttendance(UUID sessionId, UUID requesterId) {
        // Get lesson
        LessonDto lesson = scheduleApi.findLessonById(sessionId)
                .orElseThrow(() -> AttendanceErrors.lessonNotFound(sessionId));

        // Get offering
        GroupSubjectOfferingDto offering = offeringApi.findOfferingById(lesson.offeringId())
                .orElseThrow(() -> AttendanceErrors.offeringNotFound(lesson.offeringId()));

        // Check authorization: teacher must be assigned to this offering or admin
        ensureCanReadSessionAttendance(requesterId, offering);

        // Get roster
        List<StudentDto> roster = studentApi.findByGroupId(offering.groupId());

        // Get all attendance records for this session
        List<AttendanceRecord> records = repository.findByLessonSessionId(sessionId);
        Map<UUID, AttendanceRecord> recordMap = records.stream()
                .collect(Collectors.toMap(AttendanceRecord::getStudentId, r -> r));

        // Build response: for each student in roster, include attendance status (or UNMARKED)
        Map<AttendanceStatus, Integer> counts = new HashMap<>();
        counts.put(AttendanceStatus.PRESENT, 0);
        counts.put(AttendanceStatus.ABSENT, 0);
        counts.put(AttendanceStatus.LATE, 0);
        counts.put(AttendanceStatus.EXCUSED, 0);
        int unmarkedCount = 0;

        List<SessionAttendanceDto.SessionAttendanceStudentDto> studentDtos = new ArrayList<>();
        for (StudentDto student : roster) {
            AttendanceRecord record = recordMap.get(student.id());
            if (record == null) {
                unmarkedCount++;
                studentDtos.add(new SessionAttendanceDto.SessionAttendanceStudentDto(
                        student.id(),
                        null, // UNMARKED represented as null
                        null,
                        null,
                        null,
                        null
                ));
            } else {
                counts.put(record.getStatus(), counts.get(record.getStatus()) + 1);
                studentDtos.add(new SessionAttendanceDto.SessionAttendanceStudentDto(
                        student.id(),
                        record.getStatus(),
                        record.getMinutesLate(),
                        record.getTeacherComment(),
                        record.getMarkedAt(),
                        record.getMarkedBy()
                ));
            }
        }

        return new SessionAttendanceDto(sessionId, counts, unmarkedCount, studentDtos);
    }

    @Override
    @Transactional(readOnly = true)
    public StudentAttendanceDto getStudentAttendance(
            UUID studentId,
            LocalDateTime from,
            LocalDateTime to,
            UUID offeringId,
            UUID groupId,
            UUID requesterId
    ) {
        // Validate student exists
        studentApi.findById(studentId)
                .orElseThrow(() -> AttendanceErrors.studentNotFound(studentId));

        // Check authorization: student can only view own records
        UserDto requester = userApi.findById(requesterId)
                .orElseThrow(() -> AttendanceErrors.forbidden("User not found"));
        boolean isStudent = requester.hasRole(Role.STUDENT);
        boolean isTeacherOrAdmin = requester.hasRole(Role.TEACHER) || requester.hasRole(Role.ADMIN)
                || requester.hasRole(Role.MODERATOR) || requester.hasRole(Role.SUPER_ADMIN);

        if (isStudent) {
            // Student can only view own records
            StudentDto student = studentApi.findByUserId(requesterId)
                    .orElseThrow(() -> AttendanceErrors.forbidden("Student profile not found"));
            if (!student.id().equals(studentId)) {
                throw AttendanceErrors.forbidden("Students can only view their own attendance records");
            }
        } else if (!isTeacherOrAdmin) {
            throw AttendanceErrors.forbidden("Only students (own records), teachers, or administrators can view attendance");
        }

        // Get records
        List<AttendanceRecord> records = repository.findByStudentIdAndMarkedAtBetween(studentId, from, to);

        // Filter by offeringId if provided (need to get lesson's offeringId)
        if (offeringId != null) {
            // Get all lessons for this offering and filter records
            List<LessonDto> lessons = scheduleApi.findLessonsByOfferingId(offeringId);
            Set<UUID> lessonIds = lessons.stream().map(LessonDto::id).collect(Collectors.toSet());
            records = records.stream()
                    .filter(r -> lessonIds.contains(r.getLessonSessionId()))
                    .toList();
        }

        // Filter by groupId if provided (need to get lessons for group's offerings)
        if (groupId != null) {
            List<GroupSubjectOfferingDto> groupOfferings = offeringApi.findOfferingsByGroupId(groupId);
            Set<UUID> offeringIds = groupOfferings.stream()
                    .map(GroupSubjectOfferingDto::id)
                    .collect(Collectors.toSet());
            List<LessonDto> groupLessons = new ArrayList<>();
            for (UUID oid : offeringIds) {
                groupLessons.addAll(scheduleApi.findLessonsByOfferingId(oid));
            }
            Set<UUID> groupLessonIds = groupLessons.stream()
                    .map(LessonDto::id)
                    .collect(Collectors.toSet());
            records = records.stream()
                    .filter(r -> groupLessonIds.contains(r.getLessonSessionId()))
                    .toList();
        }

        // Build summary
        Map<AttendanceStatus, Integer> summary = new HashMap<>();
        summary.put(AttendanceStatus.PRESENT, 0);
        summary.put(AttendanceStatus.ABSENT, 0);
        summary.put(AttendanceStatus.LATE, 0);
        summary.put(AttendanceStatus.EXCUSED, 0);

        for (AttendanceRecord record : records) {
            summary.put(record.getStatus(), summary.get(record.getStatus()) + 1);
        }

        List<StudentAttendanceDto.StudentAttendanceRecordDto> recordDtos = records.stream()
                .map(r -> new StudentAttendanceDto.StudentAttendanceRecordDto(
                        r.getLessonSessionId(),
                        r.getStatus(),
                        r.getMinutesLate(),
                        r.getTeacherComment(),
                        r.getMarkedAt()
                ))
                .toList();

        return new StudentAttendanceDto(
                studentId,
                from,
                to,
                summary,
                records.size(),
                recordDtos
        );
    }

    @Override
    @Transactional(readOnly = true)
    public GroupAttendanceSummaryDto getGroupAttendanceSummary(
            UUID groupId,
            LocalDate from,
            LocalDate to,
            UUID offeringId,
            UUID requesterId
    ) {
        // Validate group exists
        groupApi.findGroupById(groupId)
                .orElseThrow(() -> AttendanceErrors.groupNotFound(groupId));

        // Check authorization: teacher must teach this group or admin
        ensureCanReadGroupAttendance(requesterId, groupId);

        // Get roster
        List<StudentDto> roster = studentApi.findByGroupId(groupId);
        if (roster.isEmpty()) {
            return new GroupAttendanceSummaryDto(groupId, from, to, List.of());
        }

        // Get offerings for group
        List<GroupSubjectOfferingDto> offerings = offeringApi.findOfferingsByGroupId(groupId);
        if (offeringId != null) {
            offerings = offerings.stream()
                    .filter(o -> o.id().equals(offeringId))
                    .toList();
        }

        // Get all lesson IDs for these offerings
        Set<UUID> lessonIds = new HashSet<>();
        for (GroupSubjectOfferingDto offering : offerings) {
            List<LessonDto> lessons = scheduleApi.findLessonsByOfferingId(offering.id());
            for (LessonDto lesson : lessons) {
                // Filter by date range if provided
                if (from != null && lesson.date().isBefore(from)) {
                    continue;
                }
                if (to != null && lesson.date().isAfter(to)) {
                    continue;
                }
                lessonIds.add(lesson.id());
            }
        }

        if (lessonIds.isEmpty()) {
            return new GroupAttendanceSummaryDto(groupId, from, to,
                    roster.stream()
                            .map(s -> new GroupAttendanceSummaryDto.GroupAttendanceRowDto(
                                    s.id(),
                                    Map.of(AttendanceStatus.PRESENT, 0, AttendanceStatus.ABSENT, 0,
                                            AttendanceStatus.LATE, 0, AttendanceStatus.EXCUSED, 0),
                                    0,
                                    lessonIds.size()
                            ))
                            .toList());
        }

        // Get all attendance records for these lessons
        List<AttendanceRecord> records = repository.findByLessonSessionIdIn(new ArrayList<>(lessonIds));

        // Group by student
        Map<UUID, List<AttendanceRecord>> byStudent = records.stream()
                .collect(Collectors.groupingBy(AttendanceRecord::getStudentId));

        // Build summary rows
        List<GroupAttendanceSummaryDto.GroupAttendanceRowDto> rows = new ArrayList<>();
        for (StudentDto student : roster) {
            List<AttendanceRecord> studentRecords = byStudent.getOrDefault(student.id(), List.of());
            Map<AttendanceStatus, Integer> summary = new HashMap<>();
            summary.put(AttendanceStatus.PRESENT, 0);
            summary.put(AttendanceStatus.ABSENT, 0);
            summary.put(AttendanceStatus.LATE, 0);
            summary.put(AttendanceStatus.EXCUSED, 0);

            for (AttendanceRecord record : studentRecords) {
                summary.put(record.getStatus(), summary.get(record.getStatus()) + 1);
            }

            int totalMarked = studentRecords.size();
            int unmarkedCount = (int) lessonIds.stream()
                    .filter(lid -> studentRecords.stream()
                            .noneMatch(r -> r.getLessonSessionId().equals(lid)))
                    .count();

            rows.add(new GroupAttendanceSummaryDto.GroupAttendanceRowDto(
                    student.id(),
                    summary,
                    totalMarked,
                    unmarkedCount
            ));
        }

        return new GroupAttendanceSummaryDto(groupId, from, to, rows);
    }

    /**
     * Ensure user can mark attendance for this offering (must be teacher of offering or admin).
     */
    private void ensureCanMarkAttendance(UUID userId, GroupSubjectOfferingDto offering) {
        UserDto user = userApi.findById(userId)
                .orElseThrow(() -> AttendanceErrors.forbidden("User not found"));

        // Admin/mod/moderator can always mark
        if (user.hasRole(Role.ADMIN) || user.hasRole(Role.MODERATOR) || user.hasRole(Role.SUPER_ADMIN)) {
            return;
        }

        // Teacher must be assigned to this offering
        if (user.hasRole(Role.TEACHER)) {
            // Get teacher profile for this user
            TeacherDto teacher = teacherApi.findByUserId(userId)
                    .orElseThrow(() -> AttendanceErrors.forbidden("User does not have a teacher profile"));

            // Check if teacher is main teacher of offering
            if (offering.teacherId() != null && offering.teacherId().equals(teacher.id())) {
                return;
            }

            // Check if teacher is assigned as offering teacher
            List<OfferingTeacherDto> teachers = offeringApi.findTeachersByOfferingId(offering.id());
            boolean isAssignedTeacher = teachers.stream()
                    .anyMatch(t -> t.teacherId().equals(teacher.id()));
            if (!isAssignedTeacher) {
                throw AttendanceErrors.forbidden("Only teachers assigned to this offering can mark attendance");
            }
            return;
        }

        throw AttendanceErrors.forbidden("Only teachers or administrators can mark attendance");
    }

    /**
     * Ensure user can read session attendance (must be teacher of offering or admin).
     */
    private void ensureCanReadSessionAttendance(UUID userId, GroupSubjectOfferingDto offering) {
        ensureCanMarkAttendance(userId, offering); // Same permission check
    }

    /**
     * Ensure user can read group attendance (must be teacher of group or admin).
     */
    private void ensureCanReadGroupAttendance(UUID userId, UUID groupId) {
        UserDto user = userApi.findById(userId)
                .orElseThrow(() -> AttendanceErrors.forbidden("User not found"));

        // Admin/mod/moderator can always read
        if (user.hasRole(Role.ADMIN) || user.hasRole(Role.MODERATOR) || user.hasRole(Role.SUPER_ADMIN)) {
            return;
        }

        // Teacher must teach at least one offering in this group
        if (user.hasRole(Role.TEACHER)) {
            // Get teacher profile for this user
            TeacherDto teacher = teacherApi.findByUserId(userId)
                    .orElseThrow(() -> AttendanceErrors.forbidden("User does not have a teacher profile"));

            // Check if teacher teaches any offering in this group
            List<GroupSubjectOfferingDto> offerings = offeringApi.findOfferingsByGroupId(groupId);
            boolean teachesGroup = offerings.stream()
                    .anyMatch(o -> {
                        // Check if main teacher
                        if (o.teacherId() != null && o.teacherId().equals(teacher.id())) {
                            return true;
                        }
                        // Check if assigned as offering teacher
                        List<OfferingTeacherDto> teachers = offeringApi.findTeachersByOfferingId(o.id());
                        return teachers.stream()
                                .anyMatch(t -> t.teacherId().equals(teacher.id()));
                    });
            if (!teachesGroup) {
                throw AttendanceErrors.forbidden("Only teachers of this group or administrators can view group attendance");
            }
            return;
        }

        throw AttendanceErrors.forbidden("Only teachers or administrators can view group attendance");
    }
}
