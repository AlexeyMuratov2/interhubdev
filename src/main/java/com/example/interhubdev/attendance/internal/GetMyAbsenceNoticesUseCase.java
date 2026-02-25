package com.example.interhubdev.attendance.internal;

import com.example.interhubdev.attendance.*;
import com.example.interhubdev.error.Errors;
import com.example.interhubdev.offering.GroupSubjectOfferingDto;
import com.example.interhubdev.offering.OfferingApi;
import com.example.interhubdev.offering.OfferingSlotDto;
import com.example.interhubdev.program.ProgramApi;
import com.example.interhubdev.schedule.LessonDto;
import com.example.interhubdev.schedule.ScheduleApi;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Use case for getting student's own absence notices with cursor-based pagination.
 * Returns notices for the authenticated student within an optional date range,
 * enriched with lesson, offering, and slot context for the UI.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
class GetMyAbsenceNoticesUseCase {

    private static final int MAX_LIMIT = 30;
    private static final int DEFAULT_LIMIT = 30;

    private static final LocalDateTime PG_TIMESTAMP_MIN = LocalDateTime.of(1, 1, 1, 0, 0);
    private static final LocalDateTime PG_TIMESTAMP_MAX = LocalDateTime.of(9999, 12, 31, 23, 59, 59);

    private final AbsenceNoticeRepository noticeRepository;
    private final AbsenceNoticeAttachmentRepository attachmentRepository;
    private final ScheduleApi scheduleApi;
    private final OfferingApi offeringApi;
    private final ProgramApi programApi;

    /**
     * Get absence notices for a student with cursor-based pagination and enriched context.
     *
     * @param studentId student profile ID
     * @param from      optional filter: submittedAt >= from (null to ignore)
     * @param to        optional filter: submittedAt <= to (null to ignore)
     * @param cursor    optional cursor (notice ID) for next page
     * @param limit     maximum number of results per page (capped at MAX_LIMIT)
     * @return page of enriched notice items (notice + lesson, offering, slot)
     */
    StudentAbsenceNoticePage execute(UUID studentId, LocalDateTime from, LocalDateTime to, UUID cursor, Integer limit) {
        int cappedLimit = limit == null || limit <= 0 ? DEFAULT_LIMIT : Math.min(limit, MAX_LIMIT);
        int limitPlusOne = cappedLimit + 1;

        LocalDateTime cursorSubmittedAt = null;
        UUID cursorId = null;
        if (cursor != null) {
            AbsenceNotice cursorNotice = noticeRepository.findById(cursor)
                    .orElseThrow(() -> Errors.badRequest("Cursor notice not found: " + cursor));
            if (!cursorNotice.getStudentId().equals(studentId)) {
                throw Errors.forbidden("Cursor notice does not belong to current student");
            }
            cursorSubmittedAt = cursorNotice.getSubmittedAt();
            cursorId = cursorNotice.getId();
        }

        LocalDateTime fromBound = from != null ? from : PG_TIMESTAMP_MIN;
        LocalDateTime toBound = to != null ? to : PG_TIMESTAMP_MAX;

        List<AbsenceNotice> notices;
        if (cursorSubmittedAt != null && cursorId != null) {
            notices = noticeRepository.findNextPageByStudentId(
                    studentId, fromBound, toBound, cursorSubmittedAt, cursorId,
                    PageRequest.of(0, limitPlusOne));
        } else {
            notices = noticeRepository.findFirstPageByStudentId(
                    studentId, fromBound, toBound,
                    PageRequest.of(0, limitPlusOne));
        }

        boolean hasMore = notices.size() > cappedLimit;
        List<AbsenceNotice> pageNotices = hasMore ? notices.subList(0, cappedLimit) : notices;
        UUID nextCursor = hasMore && !pageNotices.isEmpty()
                ? pageNotices.get(pageNotices.size() - 1).getId()
                : null;

        // Enrich with lesson, offering, slot (with caching)
        Map<UUID, LessonDto> lessonBySessionId = new HashMap<>();
        Map<UUID, GroupSubjectOfferingDto> offeringById = new HashMap<>();
        Map<UUID, String> subjectNameByCurriculumSubjectId = new HashMap<>();
        Map<UUID, List<OfferingSlotDto>> slotsByOfferingId = new HashMap<>();

        List<StudentAbsenceNoticeItemDto> items = new ArrayList<>();
        for (AbsenceNotice notice : pageNotices) {
            List<AbsenceNoticeAttachment> attachments = attachmentRepository
                    .findByNoticeIdOrderByCreatedAtAsc(notice.getId());
            AbsenceNoticeDto noticeDto = AbsenceNoticeMappers.toDto(notice, attachments);

            LessonDto lesson = lessonBySessionId.computeIfAbsent(notice.getLessonSessionId(),
                    sid -> scheduleApi.findLessonById(sid).orElse(null));
            GroupSubjectOfferingDto offering = lesson != null
                    ? offeringById.computeIfAbsent(lesson.offeringId(), oid -> offeringApi.findOfferingById(oid).orElse(null))
                    : null;

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

            StudentNoticeLessonSummary lessonSummary = lesson != null
                    ? new StudentNoticeLessonSummary(
                            lesson.id(),
                            lesson.offeringId(),
                            lesson.date(),
                            lesson.startTime(),
                            lesson.endTime(),
                            lesson.topic(),
                            lesson.status(),
                            lessonType)
                    : null;
            StudentNoticeOfferingSummary offeringSummary = offering != null
                    ? new StudentNoticeOfferingSummary(
                            offering.id(),
                            offering.groupId(),
                            offering.curriculumSubjectId(),
                            subjectName,
                            offering.format(),
                            offering.notes())
                    : null;
            StudentNoticeSlotSummary slotSummary = slotDto != null
                    ? new StudentNoticeSlotSummary(
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

            items.add(new StudentAbsenceNoticeItemDto(noticeDto, lessonSummary, offeringSummary, slotSummary));
        }

        return new StudentAbsenceNoticePage(items, nextCursor);
    }
}
