package com.example.interhubdev.absencenotice.internal;

import com.example.interhubdev.absencenotice.*;
import com.example.interhubdev.error.Errors;
import com.example.interhubdev.group.GroupApi;
import com.example.interhubdev.group.StudentGroupDto;
import com.example.interhubdev.offering.GroupSubjectOfferingDto;
import com.example.interhubdev.offering.OfferingApi;
import com.example.interhubdev.offering.OfferingSlotDto;
import com.example.interhubdev.program.ProgramApi;
import com.example.interhubdev.schedule.LessonDto;
import com.example.interhubdev.schedule.ScheduleApi;
import com.example.interhubdev.student.StudentApi;
import com.example.interhubdev.student.StudentDto;
import com.example.interhubdev.teacher.TeacherApi;
import com.example.interhubdev.teacher.TeacherDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
class GetTeacherAbsenceNoticesUseCase {

    private static final int MAX_LIMIT = 30;
    private static final int DEFAULT_LIMIT = 30;
    private static final int MAX_SESSION_IDS_PER_QUERY = 100;

    private final AbsenceNoticeRepository noticeRepository;
    private final AbsenceNoticeAttachmentRepository attachmentRepository;
    private final TeacherApi teacherApi;
    private final OfferingApi offeringApi;
    private final ScheduleApi scheduleApi;
    private final StudentApi studentApi;
    private final GroupApi groupApi;
    private final ProgramApi programApi;

    TeacherAbsenceNoticePage execute(UUID userId, List<AbsenceNoticeStatus> statuses, UUID cursor, Integer limit) {
        TeacherDto teacher = teacherApi.findByUserId(userId)
                .orElseThrow(() -> Errors.forbidden("User is not a teacher"));

        List<GroupSubjectOfferingDto> offerings = offeringApi.findOfferingsByTeacherId(teacher.id());
        if (offerings.isEmpty()) {
            return new TeacherAbsenceNoticePage(List.of(), null);
        }

        List<UUID> sessionIds = new ArrayList<>();
        for (GroupSubjectOfferingDto offering : offerings) {
            List<LessonDto> lessons = scheduleApi.findLessonsByOfferingId(offering.id());
            sessionIds.addAll(lessons.stream().map(LessonDto::id).toList());
        }

        if (sessionIds.isEmpty()) {
            return new TeacherAbsenceNoticePage(List.of(), null);
        }

        int cappedLimit = limit == null || limit <= 0 ? DEFAULT_LIMIT : Math.min(limit, MAX_LIMIT);

        LocalDateTime cursorSubmittedAt = null;
        UUID cursorId = null;
        if (cursor != null) {
            AbsenceNotice cursorNotice = noticeRepository.findById(cursor)
                    .orElseThrow(() -> Errors.badRequest("Cursor notice not found: " + cursor));
            cursorSubmittedAt = cursorNotice.getSubmittedAt();
            cursorId = cursorNotice.getId();
        }

        List<AbsenceNotice> notices = queryNoticesBatched(sessionIds, statuses, cursorSubmittedAt, cursorId, cappedLimit + 1);

        boolean hasMore = notices.size() > cappedLimit;
        List<AbsenceNotice> pageNotices = hasMore ? notices.subList(0, cappedLimit) : notices;
        UUID nextCursor = hasMore && !pageNotices.isEmpty()
                ? pageNotices.get(pageNotices.size() - 1).getId()
                : null;

        Map<UUID, LessonDto> lessonBySessionId = new HashMap<>();
        Map<UUID, GroupSubjectOfferingDto> offeringById = new HashMap<>();
        Map<UUID, StudentGroupDto> groupById = new HashMap<>();
        Map<UUID, StudentDto> studentById = new HashMap<>();
        Map<UUID, String> subjectNameByCurriculumSubjectId = new HashMap<>();
        Map<UUID, List<OfferingSlotDto>> slotsByOfferingId = new HashMap<>();

        List<TeacherAbsenceNoticeItemDto> items = new ArrayList<>();
        for (AbsenceNotice notice : pageNotices) {
            List<AbsenceNoticeAttachment> attachments = attachmentRepository.findByNoticeIdOrderByCreatedAtAsc(notice.getId());
            AbsenceNoticeDto noticeDto = AbsenceNoticeMappers.toDto(notice, attachments);

            LessonDto lesson = lessonBySessionId.computeIfAbsent(notice.getLessonSessionId(),
                    sid -> scheduleApi.findLessonById(sid).orElse(null));
            GroupSubjectOfferingDto offering = lesson != null
                    ? offeringById.computeIfAbsent(lesson.offeringId(), oid -> offeringApi.findOfferingById(oid).orElse(null))
                    : null;
            StudentGroupDto group = offering != null
                    ? groupById.computeIfAbsent(offering.groupId(), gid -> groupApi.findGroupById(gid).orElse(null))
                    : null;
            StudentDto student = studentById.computeIfAbsent(notice.getStudentId(),
                    sid -> studentApi.findById(sid).orElse(null));

            String subjectName = null;
            List<OfferingSlotDto> slots = null;
            if (offering != null) {
                subjectName = subjectNameByCurriculumSubjectId.computeIfAbsent(offering.curriculumSubjectId(),
                        id -> programApi.getSubjectNamesByCurriculumSubjectIds(List.of(id)).getOrDefault(id, null));
                slots = slotsByOfferingId.computeIfAbsent(offering.id(), oid -> offeringApi.findSlotsByOfferingId(oid));
            }
            OfferingSlotDto slotDto = null;
            if (lesson != null && lesson.offeringSlotId() != null && slots != null) {
                slotDto = slots.stream()
                        .filter(s -> Objects.equals(s.id(), lesson.offeringSlotId()))
                        .findFirst()
                        .orElse(null);
            }
            String lessonType = slotDto != null ? slotDto.lessonType() : null;

            TeacherNoticeStudentSummary studentSummary = student != null
                    ? new TeacherNoticeStudentSummary(
                            student.id(),
                            student.studentId(),
                            (student.chineseName() != null && !student.chineseName().isBlank()) ? student.chineseName() : student.studentId(),
                            student.groupName())
                    : null;
            TeacherNoticeLessonSummary lessonSummary = lesson != null
                    ? new TeacherNoticeLessonSummary(
                            lesson.id(),
                            lesson.offeringId(),
                            lesson.date(),
                            lesson.startTime(),
                            lesson.endTime(),
                            lesson.topic(),
                            lesson.status(),
                            lessonType)
                    : null;
            TeacherNoticeOfferingSummary offeringSummary = offering != null
                    ? new TeacherNoticeOfferingSummary(
                            offering.id(),
                            offering.groupId(),
                            offering.curriculumSubjectId(),
                            subjectName,
                            offering.format(),
                            offering.notes())
                    : null;
            TeacherNoticeSlotSummary slotSummary = slotDto != null
                    ? new TeacherNoticeSlotSummary(
                            slotDto.id(),
                            slotDto.offeringId(),
                            slotDto.dayOfWeek(),
                            slotDto.startTime(),
                            slotDto.endTime(),
                            slotDto.lessonType(),
                            slotDto.roomId(),
                            slotDto.teacherId(),
                            slotDto.timeslotId())
                    : null;
            TeacherNoticeGroupSummary groupSummary = group != null
                    ? new TeacherNoticeGroupSummary(group.id(), group.code(), group.name())
                    : null;

            items.add(new TeacherAbsenceNoticeItemDto(noticeDto, studentSummary, lessonSummary, offeringSummary, slotSummary, groupSummary));
        }

        return new TeacherAbsenceNoticePage(items, nextCursor);
    }

    private List<AbsenceNotice> queryNoticesBatched(
            List<UUID> sessionIds,
            List<AbsenceNoticeStatus> statuses,
            LocalDateTime cursorSubmittedAt,
            UUID cursorId,
            int limitPlusOne
    ) {
        boolean hasCursor = cursorSubmittedAt != null && cursorId != null;
        boolean hasStatusFilter = statuses != null && !statuses.isEmpty();

        List<AbsenceNotice> merged = new ArrayList<>();
        for (int i = 0; i < sessionIds.size(); i += MAX_SESSION_IDS_PER_QUERY) {
            int to = Math.min(i + MAX_SESSION_IDS_PER_QUERY, sessionIds.size());
            List<UUID> chunk = sessionIds.subList(i, to);
            List<AbsenceNotice> chunkResult;
            if (hasStatusFilter) {
                chunkResult = hasCursor
                        ? noticeRepository.findNextPageBySessionIdsAndStatuses(chunk, statuses, cursorSubmittedAt, cursorId)
                        : noticeRepository.findFirstPageBySessionIdsAndStatuses(chunk, statuses);
            } else {
                chunkResult = hasCursor
                        ? noticeRepository.findNextPageBySessionIds(chunk, cursorSubmittedAt, cursorId)
                        : noticeRepository.findFirstPageBySessionIds(chunk);
            }
            merged.addAll(chunkResult);
        }

        Comparator<AbsenceNotice> order = Comparator
                .comparing(AbsenceNotice::getSubmittedAt, Comparator.reverseOrder())
                .thenComparing(AbsenceNotice::getId, Comparator.reverseOrder());
        merged.sort(order);

        return merged.size() <= limitPlusOne ? merged : merged.subList(0, limitPlusOne);
    }
}
