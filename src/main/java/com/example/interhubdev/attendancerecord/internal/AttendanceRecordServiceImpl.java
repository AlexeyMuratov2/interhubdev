package com.example.interhubdev.attendancerecord.internal;

import com.example.interhubdev.attendancerecord.*;
import com.example.interhubdev.group.GroupApi;
import com.example.interhubdev.offering.GroupSubjectOfferingDto;
import com.example.interhubdev.offering.OfferingApi;
import com.example.interhubdev.offering.OfferingTeacherItemDto;
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

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Implementation of {@link AttendanceRecordApi}.
 */
@Service
@RequiredArgsConstructor
class AttendanceRecordServiceImpl implements AttendanceRecordApi {

    private final AttendanceRecordRepository repository;
    private final ScheduleApi scheduleApi;
    private final OfferingApi offeringApi;
    private final StudentApi studentApi;
    private final GroupApi groupApi;
    private final TeacherApi teacherApi;
    private final UserApi userApi;
    private final SessionGateway sessionGateway;
    private final RosterGateway rosterGateway;
    private final AttendanceRecordAccessPolicy accessPolicy;

    @Override
    @Transactional
    public List<AttendanceRecordDto> markAttendanceBulk(UUID sessionId, List<MarkAttendanceItem> items, UUID markedBy) {
        if (items == null || items.isEmpty()) {
            return List.of();
        }

        LessonDto lesson = sessionGateway.getSessionById(sessionId)
                .orElseThrow(() -> AttendanceRecordErrors.lessonNotFound(sessionId));

        GroupSubjectOfferingDto offering = offeringApi.findOfferingById(lesson.offeringId())
                .orElseThrow(() -> AttendanceRecordErrors.offeringNotFound(lesson.offeringId()));

        ensureCanMarkAttendance(markedBy, offering);

        Set<UUID> rosterStudentIds = rosterGateway.getStudentIdsByGroupId(offering.groupId());

        LocalDateTime now = LocalDateTime.now();
        List<AttendanceRecord> toSave = new ArrayList<>();

        for (MarkAttendanceItem item : items) {
            if (!rosterStudentIds.contains(item.studentId())) {
                throw AttendanceRecordErrors.studentNotInGroup(item.studentId(), offering.groupId());
            }
            studentApi.findById(item.studentId())
                    .orElseThrow(() -> AttendanceRecordErrors.studentNotFound(item.studentId()));

            AttendanceRecordValidation.validateStatusAndMinutesLate(item.status(), item.minutesLate());
            AttendanceRecordValidation.validateTeacherComment(item.teacherComment());

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

            record.setAbsenceNoticeId(item.absenceNoticeId());
            toSave.add(record);
        }

        List<AttendanceRecord> saved = repository.saveAll(toSave);
        return saved.stream()
                .map(AttendanceRecordMappers::toDto)
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
            UUID absenceNoticeId,
            UUID markedBy
    ) {
        LessonDto lesson = sessionGateway.getSessionById(sessionId)
                .orElseThrow(() -> AttendanceRecordErrors.lessonNotFound(sessionId));

        GroupSubjectOfferingDto offering = offeringApi.findOfferingById(lesson.offeringId())
                .orElseThrow(() -> AttendanceRecordErrors.offeringNotFound(lesson.offeringId()));

        ensureCanMarkAttendance(markedBy, offering);

        List<StudentDto> roster = rosterGateway.getRosterByGroupId(offering.groupId());
        boolean studentInRoster = roster.stream().anyMatch(s -> s.id().equals(studentId));
        if (!studentInRoster) {
            throw AttendanceRecordErrors.studentNotInGroup(studentId, offering.groupId());
        }
        studentApi.findById(studentId)
                .orElseThrow(() -> AttendanceRecordErrors.studentNotFound(studentId));

        AttendanceRecordValidation.validateStatusAndMinutesLate(status, minutesLate);
        AttendanceRecordValidation.validateTeacherComment(teacherComment);

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

        record.setAbsenceNoticeId(absenceNoticeId);
        AttendanceRecord saved = repository.save(record);
        return AttendanceRecordMappers.toDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public SessionRecordsDto getSessionRecords(UUID sessionId, UUID requesterId) {
        LessonDto lesson = sessionGateway.getSessionById(sessionId)
                .orElseThrow(() -> AttendanceRecordErrors.lessonNotFound(sessionId));

        GroupSubjectOfferingDto offering = offeringApi.findOfferingById(lesson.offeringId())
                .orElseThrow(() -> AttendanceRecordErrors.offeringNotFound(lesson.offeringId()));

        accessPolicy.ensureCanManageSession(requesterId, lesson);

        List<StudentDto> roster = rosterGateway.getRosterByGroupId(offering.groupId());
        List<AttendanceRecord> records = repository.findByLessonSessionId(sessionId);
        Map<UUID, AttendanceRecord> recordMap = records.stream()
                .collect(Collectors.toMap(AttendanceRecord::getStudentId, r -> r));

        Map<AttendanceStatus, Integer> counts = new HashMap<>();
        counts.put(AttendanceStatus.PRESENT, 0);
        counts.put(AttendanceStatus.ABSENT, 0);
        counts.put(AttendanceStatus.LATE, 0);
        counts.put(AttendanceStatus.EXCUSED, 0);
        int unmarkedCount = 0;

        List<SessionRecordsDto.SessionRecordRowDto> rows = new ArrayList<>();
        for (StudentDto student : roster) {
            AttendanceRecord record = recordMap.get(student.id());
            if (record == null) {
                unmarkedCount++;
                rows.add(new SessionRecordsDto.SessionRecordRowDto(
                        student.id(),
                        null,
                        null,
                        null,
                        null,
                        null,
                        Optional.empty()
                ));
            } else {
                counts.put(record.getStatus(), counts.get(record.getStatus()) + 1);
                rows.add(new SessionRecordsDto.SessionRecordRowDto(
                        student.id(),
                        record.getStatus(),
                        record.getMinutesLate(),
                        record.getTeacherComment(),
                        record.getMarkedAt(),
                        record.getMarkedBy(),
                        Optional.ofNullable(record.getAbsenceNoticeId())
                ));
            }
        }

        return new SessionRecordsDto(sessionId, counts, unmarkedCount, rows);
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
        studentApi.findById(studentId)
                .orElseThrow(() -> AttendanceRecordErrors.studentNotFound(studentId));

        UserDto requester = userApi.findById(requesterId)
                .orElseThrow(() -> AttendanceRecordErrors.forbidden("User not found"));
        boolean isStudent = requester.hasRole(Role.STUDENT);
        boolean isTeacherOrAdmin = requester.hasRole(Role.TEACHER) || requester.hasRole(Role.ADMIN)
                || requester.hasRole(Role.MODERATOR) || requester.hasRole(Role.SUPER_ADMIN);

        if (isStudent) {
            StudentDto student = studentApi.findByUserId(requesterId)
                    .orElseThrow(() -> AttendanceRecordErrors.forbidden("Student profile not found"));
            if (!student.id().equals(studentId)) {
                throw AttendanceRecordErrors.forbidden("Students can only view their own attendance records");
            }
        } else if (!isTeacherOrAdmin) {
            throw AttendanceRecordErrors.forbidden("Only students (own records), teachers, or administrators can view attendance");
        }

        List<AttendanceRecord> records = getRecordsByDateRange(studentId, from, to);

        if (offeringId != null) {
            List<LessonDto> lessons = scheduleApi.findLessonsByOfferingId(offeringId);
            Set<UUID> lessonIds = lessons.stream().map(LessonDto::id).collect(Collectors.toSet());
            records = records.stream()
                    .filter(r -> lessonIds.contains(r.getLessonSessionId()))
                    .toList();
        }

        if (groupId != null) {
            List<GroupSubjectOfferingDto> groupOfferings = offeringApi.findOfferingsByGroupId(groupId);
            Set<UUID> offeringIds = groupOfferings.stream()
                    .map(GroupSubjectOfferingDto::id)
                    .collect(Collectors.toSet());
            List<LessonDto> groupLessons = new ArrayList<>();
            for (UUID oid : offeringIds) {
                groupLessons.addAll(scheduleApi.findLessonsByOfferingId(oid));
            }
            Set<UUID> groupLessonIds = groupLessons.stream().map(LessonDto::id).collect(Collectors.toSet());
            records = records.stream()
                    .filter(r -> groupLessonIds.contains(r.getLessonSessionId()))
                    .toList();
        }

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

        return new StudentAttendanceDto(studentId, from, to, summary, records.size(), recordDtos);
    }

    @Override
    @Transactional(readOnly = true)
    public StudentAttendanceRecordsByLessonsDto getStudentAttendanceByLessonIds(
            UUID studentId,
            List<UUID> lessonIds,
            UUID requesterId
    ) {
        studentApi.findById(studentId)
                .orElseThrow(() -> AttendanceRecordErrors.studentNotFound(studentId));

        UserDto requester = userApi.findById(requesterId)
                .orElseThrow(() -> AttendanceRecordErrors.forbidden("User not found"));
        boolean isStudent = requester.hasRole(Role.STUDENT);
        boolean isTeacherOrAdmin = requester.hasRole(Role.TEACHER) || requester.hasRole(Role.ADMIN)
                || requester.hasRole(Role.MODERATOR) || requester.hasRole(Role.SUPER_ADMIN);

        if (isStudent) {
            StudentDto student = studentApi.findByUserId(requesterId)
                    .orElseThrow(() -> AttendanceRecordErrors.forbidden("Student profile not found"));
            if (!student.id().equals(studentId)) {
                throw AttendanceRecordErrors.forbidden("Students can only view their own attendance records");
            }
        } else if (!isTeacherOrAdmin) {
            throw AttendanceRecordErrors.forbidden("Only students (own records), teachers, or administrators can view attendance");
        }

        if (lessonIds == null || lessonIds.isEmpty()) {
            return new StudentAttendanceRecordsByLessonsDto(List.of());
        }

        List<AttendanceRecord> records = repository.findByStudentIdAndLessonSessionIdIn(studentId, lessonIds);
        Map<UUID, AttendanceRecord> recordByLesson = records.stream()
                .collect(Collectors.toMap(AttendanceRecord::getLessonSessionId, r -> r, (a, b) -> a));

        List<LessonAttendanceRecordItemDto> items = new ArrayList<>();
        for (UUID lessonId : lessonIds) {
            AttendanceRecord rec = recordByLesson.get(lessonId);
            items.add(new LessonAttendanceRecordItemDto(
                    lessonId,
                    rec != null ? Optional.of(AttendanceRecordMappers.toDto(rec)) : Optional.empty()
            ));
        }
        return new StudentAttendanceRecordsByLessonsDto(items);
    }

    @Override
    @Transactional(readOnly = true)
    public GroupAttendanceSummaryDto getGroupAttendanceSummary(
            UUID groupId,
            java.time.LocalDate from,
            java.time.LocalDate to,
            UUID offeringId,
            UUID requesterId
    ) {
        groupApi.findGroupById(groupId)
                .orElseThrow(() -> AttendanceRecordErrors.groupNotFound(groupId));

        ensureCanReadGroupAttendance(requesterId, groupId);

        List<StudentDto> roster = studentApi.findByGroupId(groupId);
        if (roster.isEmpty()) {
            return new GroupAttendanceSummaryDto(groupId, from, to, List.of());
        }

        List<GroupSubjectOfferingDto> offerings = offeringApi.findOfferingsByGroupId(groupId);
        if (offeringId != null) {
            offerings = offerings.stream()
                    .filter(o -> o.id().equals(offeringId))
                    .toList();
        }

        Set<UUID> lessonIds = new HashSet<>();
        for (GroupSubjectOfferingDto offering : offerings) {
            List<LessonDto> lessons = scheduleApi.findLessonsByOfferingId(offering.id());
            for (LessonDto lesson : lessons) {
                if (from != null && lesson.date().isBefore(from)) continue;
                if (to != null && lesson.date().isAfter(to)) continue;
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
                                    0,
                                    null
                            ))
                            .toList());
        }

        List<AttendanceRecord> records = repository.findByLessonSessionIdIn(new ArrayList<>(lessonIds));
        Set<UUID> lessonIdsWithAnyMark = records.stream()
                .map(AttendanceRecord::getLessonSessionId)
                .collect(Collectors.toSet());
        Map<UUID, List<AttendanceRecord>> byStudent = records.stream()
                .collect(Collectors.groupingBy(AttendanceRecord::getStudentId));

        int sessionsWithAtLeastOneMark = lessonIdsWithAnyMark.size();
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

            int present = summary.getOrDefault(AttendanceStatus.PRESENT, 0);
            int late = summary.getOrDefault(AttendanceStatus.LATE, 0);
            Double attendancePercent = sessionsWithAtLeastOneMark > 0
                    ? (present + late) * 100.0 / sessionsWithAtLeastOneMark
                    : null;

            rows.add(new GroupAttendanceSummaryDto.GroupAttendanceRowDto(
                    student.id(),
                    summary,
                    totalMarked,
                    unmarkedCount,
                    attendancePercent
            ));
        }

        return new GroupAttendanceSummaryDto(groupId, from, to, rows);
    }

    @Override
    @Transactional
    public AttendanceRecordDto attachNotice(UUID recordId, UUID noticeId, UUID requesterId) {
        AttendanceRecord record = repository.findById(recordId)
                .orElseThrow(() -> AttendanceRecordErrors.recordNotFound(recordId));

        LessonDto lesson = sessionGateway.getSessionById(record.getLessonSessionId())
                .orElseThrow(() -> AttendanceRecordErrors.lessonNotFound(record.getLessonSessionId()));
        accessPolicy.ensureCanManageSession(requesterId, lesson);

        record.setAbsenceNoticeId(noticeId);
        AttendanceRecord saved = repository.save(record);
        return AttendanceRecordMappers.toDto(saved);
    }

    @Override
    @Transactional
    public AttendanceRecordDto detachNotice(UUID recordId, UUID requesterId) {
        AttendanceRecord record = repository.findById(recordId)
                .orElseThrow(() -> AttendanceRecordErrors.recordNotFound(recordId));

        LessonDto lesson = sessionGateway.getSessionById(record.getLessonSessionId())
                .orElseThrow(() -> AttendanceRecordErrors.lessonNotFound(record.getLessonSessionId()));
        accessPolicy.ensureCanManageSession(requesterId, lesson);

        record.setAbsenceNoticeId(null);
        AttendanceRecord saved = repository.save(record);
        return AttendanceRecordMappers.toDto(saved);
    }

    private List<AttendanceRecord> getRecordsByDateRange(UUID studentId, LocalDateTime from, LocalDateTime to) {
        if (from == null && to == null) {
            return repository.findByStudentIdOrderByMarkedAtDesc(studentId);
        }
        if (from == null) {
            return repository.findByStudentIdAndMarkedAtLessThanEqualOrderByMarkedAtDesc(studentId, to);
        }
        if (to == null) {
            return repository.findByStudentIdAndMarkedAtGreaterThanEqualOrderByMarkedAtDesc(studentId, from);
        }
        return repository.findByStudentIdAndMarkedAtBetweenOrderByMarkedAtDesc(studentId, from, to);
    }

    private void ensureCanMarkAttendance(UUID userId, GroupSubjectOfferingDto offering) {
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
                throw AttendanceRecordErrors.forbidden("Only teachers assigned to this offering can mark attendance");
            }
            return;
        }

        throw AttendanceRecordErrors.forbidden("Only teachers or administrators can mark attendance");
    }

    private void ensureCanReadGroupAttendance(UUID userId, UUID groupId) {
        UserDto user = userApi.findById(userId)
                .orElseThrow(() -> AttendanceRecordErrors.forbidden("User not found"));

        if (user.hasRole(Role.ADMIN) || user.hasRole(Role.MODERATOR) || user.hasRole(Role.SUPER_ADMIN)) {
            return;
        }

        if (user.hasRole(Role.TEACHER)) {
            TeacherDto teacher = teacherApi.findByUserId(userId)
                    .orElseThrow(() -> AttendanceRecordErrors.forbidden("User does not have a teacher profile"));

            List<GroupSubjectOfferingDto> offerings = offeringApi.findOfferingsByGroupId(groupId);
            boolean teachesGroup = offerings.stream()
                    .anyMatch(o -> {
                        if (o.teacherId() != null && o.teacherId().equals(teacher.id())) {
                            return true;
                        }
                        List<OfferingTeacherItemDto> teachers = offeringApi.findTeachersByOfferingId(o.id());
                        return teachers.stream().anyMatch(t -> t.teacherId().equals(teacher.id()));
                    });
            if (!teachesGroup) {
                throw AttendanceRecordErrors.forbidden("Only teachers of this group or administrators can view group attendance");
            }
            return;
        }

        throw AttendanceRecordErrors.forbidden("Only teachers or administrators can view group attendance");
    }
}
