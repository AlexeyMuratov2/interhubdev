package com.example.interhubdev.grades.internal;

import com.example.interhubdev.grades.*;
import com.example.interhubdev.offering.OfferingApi;
import com.example.interhubdev.offering.GroupSubjectOfferingDto;
import com.example.interhubdev.schedule.ScheduleApi;
import com.example.interhubdev.student.StudentApi;
import com.example.interhubdev.student.StudentDto;
import com.example.interhubdev.group.GroupApi;
import com.example.interhubdev.user.Role;
import com.example.interhubdev.user.UserApi;
import com.example.interhubdev.user.UserDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Implementation of {@link GradesApi}: create, update, void, and query grade entries.
 */
@Service
@RequiredArgsConstructor
class GradesServiceImpl implements GradesApi {

    private final GradeEntryRepository repository;
    private final OfferingApi offeringApi;
    private final ScheduleApi scheduleApi;
    private final StudentApi studentApi;
    private final GroupApi groupApi;
    private final UserApi userApi;

    @Override
    @Transactional
    public GradeEntryDto create(
            UUID studentId,
            UUID offeringId,
            BigDecimal points,
            GradeTypeCode typeCode,
            String typeLabel,
            String description,
            UUID lessonSessionId,
            UUID homeworkSubmissionId,
            LocalDateTime gradedAt,
            UUID gradedBy
    ) {
        ensureCanGrade(gradedBy);
        validateOfferingExists(offeringId);
        validateStudentExists(studentId);
        GradeValidation.validateTypeAndLabel(typeCode, typeLabel);
        GradeValidation.validateDescription(description);
        GradeValidation.validatePoints(points);

        LocalDateTime at = gradedAt != null ? gradedAt : LocalDateTime.now();
        GradeEntryEntity entity = GradeEntryEntity.builder()
                .studentId(studentId)
                .offeringId(offeringId)
                .points(points)
                .typeCode(typeCode)
                .typeLabel(typeCode == GradeTypeCode.CUSTOM ? typeLabel : null)
                .description(description)
                .lessonId(lessonSessionId)
                .homeworkSubmissionId(homeworkSubmissionId)
                .gradedBy(gradedBy)
                .gradedAt(at)
                .status(GradeEntryEntity.STATUS_ACTIVE)
                .build();
        GradeEntryEntity saved = repository.save(entity);
        return GradeEntryMappers.toDto(saved);
    }

    @Override
    @Transactional
    public List<GradeEntryDto> createBulk(
            UUID offeringId,
            GradeTypeCode typeCode,
            String typeLabel,
            String description,
            UUID lessonSessionId,
            LocalDateTime gradedAt,
            List<BulkGradeItem> items,
            UUID gradedBy
    ) {
        ensureCanGrade(gradedBy);
        validateOfferingExists(offeringId);
        GradeValidation.validateTypeAndLabel(typeCode, typeLabel);
        GradeValidation.validateDescription(description);

        LocalDateTime at = gradedAt != null ? gradedAt : LocalDateTime.now();
        List<GradeEntryEntity> toSave = new ArrayList<>();
        for (BulkGradeItem item : items) {
            validateStudentExists(item.studentId());
            GradeValidation.validatePoints(item.points());
            GradeEntryEntity entity = GradeEntryEntity.builder()
                    .studentId(item.studentId())
                    .offeringId(offeringId)
                    .points(item.points())
                    .typeCode(typeCode)
                    .typeLabel(typeCode == GradeTypeCode.CUSTOM ? typeLabel : null)
                    .description(description)
                    .lessonId(lessonSessionId)
                    .homeworkSubmissionId(item.homeworkSubmissionId())
                    .gradedBy(gradedBy)
                    .gradedAt(at)
                    .status(GradeEntryEntity.STATUS_ACTIVE)
                    .build();
            toSave.add(entity);
        }
        List<GradeEntryEntity> saved = repository.saveAll(toSave);
        return saved.stream().map(GradeEntryMappers::toDto).toList();
    }

    @Override
    @Transactional
    public GradeEntryDto update(
            UUID id,
            BigDecimal points,
            GradeTypeCode typeCode,
            String typeLabel,
            String description,
            UUID lessonSessionId,
            UUID homeworkSubmissionId,
            LocalDateTime gradedAt,
            UUID requesterId
    ) {
        ensureCanGrade(requesterId);
        GradeEntryEntity entity = repository.findById(id)
                .orElseThrow(() -> GradeErrors.entryNotFound(id));
        if (GradeEntryEntity.STATUS_VOIDED.equals(entity.getStatus())) {
            throw GradeErrors.entryVoided(id);
        }
        if (points != null) {
            GradeValidation.validatePoints(points);
            entity.setPoints(points);
        }
        if (typeCode != null) {
            GradeValidation.validateTypeAndLabel(typeCode, typeLabel);
            entity.setTypeCode(typeCode);
            entity.setTypeLabel(typeCode == GradeTypeCode.CUSTOM ? typeLabel : null);
        } else if (typeLabel != null && entity.getTypeCode() == GradeTypeCode.CUSTOM) {
            GradeValidation.validateTypeAndLabel(GradeTypeCode.CUSTOM, typeLabel);
            entity.setTypeLabel(typeLabel);
        }
        if (description != null) {
            GradeValidation.validateDescription(description);
            entity.setDescription(description);
        }
        if (lessonSessionId != null) {
            entity.setLessonId(lessonSessionId);
        }
        if (homeworkSubmissionId != null) {
            entity.setHomeworkSubmissionId(homeworkSubmissionId);
        }
        if (gradedAt != null) {
            entity.setGradedAt(gradedAt);
        }
        GradeEntryEntity saved = repository.save(entity);
        return GradeEntryMappers.toDto(saved);
    }

    @Override
    @Transactional
    public void voidEntry(UUID id, UUID gradedBy) {
        ensureCanGrade(gradedBy);
        GradeEntryEntity entity = repository.findById(id)
                .orElseThrow(() -> GradeErrors.entryNotFound(id));
        entity.setStatus(GradeEntryEntity.STATUS_VOIDED);
        repository.save(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<GradeEntryDto> getById(UUID id, UUID requesterId) {
        ensureCanGrade(requesterId);
        return repository.findById(id).map(GradeEntryMappers::toDto);
    }

    /** Safe bounds for DB timestamp range (PostgreSQL rejects LocalDateTime.MIN/MAX). */
    private static final LocalDateTime SAFE_MIN = LocalDateTime.of(1970, 1, 1, 0, 0);
    private static final LocalDateTime SAFE_MAX = LocalDateTime.of(9999, 12, 31, 23, 59, 59);

    @Override
    @Transactional(readOnly = true)
    public StudentOfferingGradesDto getStudentOfferingGrades(
            UUID studentId,
            UUID offeringId,
            LocalDateTime from,
            LocalDateTime to,
            boolean includeVoided,
            UUID requesterId
    ) {
        ensureCanGrade(requesterId);
        validateOfferingExists(offeringId);
        List<GradeEntryEntity> list;
        if (from == null && to == null) {
            list = repository.findByStudentIdAndOfferingIdOrderByGradedAtDesc(studentId, offeringId);
        } else {
            LocalDateTime fromBound = from != null ? from : SAFE_MIN;
            LocalDateTime toBound = to != null ? to : SAFE_MAX;
            list = repository.findByStudentIdAndOfferingIdAndGradedAtBetween(
                    studentId, offeringId, fromBound, toBound);
        }
        List<GradeEntryDto> entries = list.stream()
                .filter(e -> includeVoided || GradeEntryEntity.STATUS_ACTIVE.equals(e.getStatus()))
                .map(GradeEntryMappers::toDto)
                .toList();
        List<GradeEntryEntity> activeOnly = list.stream()
                .filter(e -> GradeEntryEntity.STATUS_ACTIVE.equals(e.getStatus()))
                .toList();
        BigDecimal total = activeOnly.stream()
                .map(GradeEntryEntity::getPoints)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        Map<String, BigDecimal> breakdown = activeOnly.stream()
                .collect(Collectors.groupingBy(e -> e.getTypeCode().name(),
                        Collectors.reducing(BigDecimal.ZERO, GradeEntryEntity::getPoints, BigDecimal::add)));
        return new StudentOfferingGradesDto(studentId, offeringId, entries, total, breakdown);
    }

    @Override
    @Transactional(readOnly = true)
    public GroupOfferingSummaryDto getGroupOfferingSummary(
            UUID groupId,
            UUID offeringId,
            LocalDateTime from,
            LocalDateTime to,
            boolean includeVoided,
            UUID requesterId
    ) {
        ensureCanGrade(requesterId);
        if (groupApi.findGroupById(groupId).isEmpty()) {
            throw GradeErrors.groupNotFound(groupId);
        }
        GroupSubjectOfferingDto offering = offeringApi.findOfferingById(offeringId)
                .orElseThrow(() -> GradeErrors.offeringNotFound(offeringId));
        if (!offering.groupId().equals(groupId)) {
            throw GradeErrors.offeringNotForGroup(offeringId, groupId);
        }
        List<StudentDto> students = studentApi.findByGroupId(groupId);
        if (students.isEmpty()) {
            return new GroupOfferingSummaryDto(groupId, offeringId, List.of());
        }
        List<UUID> studentIds = students.stream().map(StudentDto::id).toList();
        List<GradeEntryEntity> allEntries = repository.findByOfferingIdAndStudentIdInOrderByStudentIdAscGradedAtDesc(
                offeringId, studentIds);
        if (from != null || to != null) {
            allEntries = allEntries.stream()
                    .filter(e -> (from == null || !e.getGradedAt().isBefore(from))
                            && (to == null || !e.getGradedAt().isAfter(to)))
                    .toList();
        }
        Map<UUID, List<GradeEntryEntity>> byStudent = allEntries.stream()
                .collect(Collectors.groupingBy(GradeEntryEntity::getStudentId));

        List<GroupOfferingSummaryRow> rows = new ArrayList<>();
        for (UUID sid : studentIds) {
            List<GradeEntryEntity> studentEntries = byStudent.getOrDefault(sid, List.of());
            List<GradeEntryEntity> activeOnly = studentEntries.stream()
                    .filter(e -> GradeEntryEntity.STATUS_ACTIVE.equals(e.getStatus()))
                    .toList();
            BigDecimal total = activeOnly.stream()
                    .map(GradeEntryEntity::getPoints)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            Map<String, BigDecimal> breakdown = activeOnly.stream()
                    .collect(Collectors.groupingBy(e -> e.getTypeCode().name(),
                            Collectors.reducing(BigDecimal.ZERO, GradeEntryEntity::getPoints, BigDecimal::add)));
            rows.add(new GroupOfferingSummaryRow(sid, total, breakdown));
        }
        return new GroupOfferingSummaryDto(groupId, offeringId, rows);
    }

    @Override
    @Transactional(readOnly = true)
    public LessonGradesSummaryDto getLessonGradesSummary(UUID lessonSessionId, UUID requesterId) {
        ensureCanGrade(requesterId);
        // Only entries without homework submission (lesson points only; homework points are separate).
        List<GradeEntryEntity> entries = repository.findByLessonIdAndHomeworkSubmissionIdIsNullAndStatusOrderByStudentIdAsc(
                lessonSessionId, GradeEntryEntity.STATUS_ACTIVE);
        Map<UUID, BigDecimal> pointsByStudent = entries.stream()
                .collect(Collectors.groupingBy(GradeEntryEntity::getStudentId,
                        Collectors.reducing(BigDecimal.ZERO, GradeEntryEntity::getPoints, BigDecimal::add)));
        List<LessonGradesSummaryDto.LessonGradeRowDto> rows = pointsByStudent.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(e -> new LessonGradesSummaryDto.LessonGradeRowDto(e.getKey(), e.getValue()))
                .toList();
        return new LessonGradesSummaryDto(lessonSessionId, rows);
    }

    @Override
    @Transactional
    public GradeEntryDto setPointsForLesson(UUID lessonSessionId, UUID studentId, BigDecimal points, UUID requesterId) {
        ensureCanGrade(requesterId);
        GradeValidation.validatePoints(points);

        var lesson = scheduleApi.findLessonById(lessonSessionId)
                .orElseThrow(() -> GradeErrors.lessonNotFound(lessonSessionId));
        var offering = offeringApi.findOfferingById(lesson.offeringId())
                .orElseThrow(() -> GradeErrors.offeringNotFound(lesson.offeringId()));
        validateStudentExists(studentId);

        List<StudentDto> roster = studentApi.findByGroupId(offering.groupId());
        boolean inGroup = roster.stream().anyMatch(s -> s.id().equals(studentId));
        if (!inGroup) {
            throw GradeErrors.studentNotInGroup(studentId, offering.groupId());
        }

        // Only lesson-only entries (exclude entries linked to homework submission).
        List<GradeEntryEntity> existing = repository.findByLessonIdAndStudentIdAndHomeworkSubmissionIdIsNullAndStatusOrderByGradedAtDesc(
                lessonSessionId, studentId, GradeEntryEntity.STATUS_ACTIVE);

        if (existing.isEmpty()) {
            GradeEntryEntity created = GradeEntryEntity.builder()
                    .studentId(studentId)
                    .offeringId(offering.id())
                    .points(points)
                    .typeCode(GradeTypeCode.OTHER)
                    .typeLabel(null)
                    .description(null)
                    .lessonId(lessonSessionId)
                    .homeworkSubmissionId(null)
                    .gradedBy(requesterId)
                    .gradedAt(LocalDateTime.now())
                    .status(GradeEntryEntity.STATUS_ACTIVE)
                    .build();
            GradeEntryEntity saved = repository.save(created);
            return GradeEntryMappers.toDto(saved);
        }

        GradeEntryEntity first = existing.get(0);
        first.setPoints(points);
        repository.save(first);
        for (int i = 1; i < existing.size(); i++) {
            GradeEntryEntity e = existing.get(i);
            e.setStatus(GradeEntryEntity.STATUS_VOIDED);
            repository.save(e);
        }
        return GradeEntryMappers.toDto(first);
    }

    private void ensureCanGrade(UUID userId) {
        UserDto user = userApi.findById(userId)
                .orElseThrow(GradeErrors::forbidden);
        if (user.hasRole(Role.TEACHER) || user.hasRole(Role.ADMIN) || user.hasRole(Role.MODERATOR) || user.hasRole(Role.SUPER_ADMIN)) {
            return;
        }
        throw GradeErrors.forbidden();
    }

    private void validateOfferingExists(UUID offeringId) {
        if (offeringApi.findOfferingById(offeringId).isEmpty()) {
            throw GradeErrors.offeringNotFound(offeringId);
        }
    }

    private void validateStudentExists(UUID studentId) {
        if (studentApi.findById(studentId).isEmpty()) {
            throw GradeErrors.studentNotFound(studentId);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Map<UUID, BigDecimal> getPointsByHomeworkSubmissionIds(List<UUID> submissionIds, UUID requesterId) {
        ensureCanGrade(requesterId);
        if (submissionIds == null || submissionIds.isEmpty()) {
            return Map.of();
        }
        List<GradeEntryEntity> entries = repository.findByHomeworkSubmissionIdInAndStatus(
                submissionIds, GradeEntryEntity.STATUS_ACTIVE);
        return entries.stream()
                .collect(Collectors.groupingBy(GradeEntryEntity::getHomeworkSubmissionId,
                        Collectors.reducing(BigDecimal.ZERO, GradeEntryEntity::getPoints, BigDecimal::add)));
    }

    @Override
    @Transactional(readOnly = true)
    public Map<UUID, GradeEntryDto> getGradeEntriesByHomeworkSubmissionIds(List<UUID> submissionIds, UUID requesterId) {
        ensureCanGrade(requesterId);
        if (submissionIds == null || submissionIds.isEmpty()) {
            return Map.of();
        }
        List<GradeEntryEntity> entries = repository.findByHomeworkSubmissionIdInAndStatus(
                submissionIds, GradeEntryEntity.STATUS_ACTIVE);
        return entries.stream()
                .collect(Collectors.groupingBy(GradeEntryEntity::getHomeworkSubmissionId))
                .entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> GradeEntryMappers.toDto(
                                e.getValue().stream()
                                        .max((a, b) -> a.getGradedAt().compareTo(b.getGradedAt()))
                                        .orElseThrow()
                        )
                ));
    }
}
