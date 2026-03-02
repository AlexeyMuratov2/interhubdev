package com.example.interhubdev.absencenotice.internal;

import com.example.interhubdev.absencenotice.*;
import com.example.interhubdev.error.Errors;
import com.example.interhubdev.schedule.LessonDto;
import com.example.interhubdev.schedule.ScheduleApi;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
class GetMyAbsenceNoticesUseCase {

    private static final int MAX_LIMIT = 30;
    private static final int DEFAULT_LIMIT = 30;
    private static final LocalDateTime PG_TIMESTAMP_MIN = LocalDateTime.of(1, 1, 1, 0, 0);
    private static final LocalDateTime PG_TIMESTAMP_MAX = LocalDateTime.of(9999, 12, 31, 23, 59, 59);

    private final AbsenceNoticeRepository noticeRepository;
    private final AbsenceNoticeLessonRepository lessonRepository;
    private final AbsenceNoticeAttachmentRepository attachmentRepository;
    private final ScheduleApi scheduleApi;

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

        List<StudentAbsenceNoticeItemDto> items = new ArrayList<>();
        for (AbsenceNotice notice : pageNotices) {
            List<AbsenceNoticeLesson> lessons = lessonRepository.findByNoticeIdOrderByLessonSessionId(notice.getId());
            List<UUID> lessonIds = lessons.stream().map(AbsenceNoticeLesson::getLessonSessionId).toList();
            List<AbsenceNoticeAttachment> attachments = attachmentRepository.findByNoticeIdOrderByCreatedAtAsc(notice.getId());
            AbsenceNoticeDto noticeDto = AbsenceNoticeMappers.toDto(notice, lessonIds, attachments);
            List<LessonDto> lessonDtos = lessonIds.isEmpty() ? List.of() : scheduleApi.findLessonsByIds(lessonIds);
            StudentNoticePeriodSummary period = buildPeriodSummary(lessonDtos);
            items.add(new StudentAbsenceNoticeItemDto(noticeDto, period));
        }

        return new StudentAbsenceNoticePage(items, nextCursor);
    }

    private StudentNoticePeriodSummary buildPeriodSummary(List<LessonDto> lessons) {
        if (lessons == null || lessons.isEmpty()) {
            return null;
        }
        LocalDateTime startAt = lessons.stream()
                .map(this::lessonStartAt)
                .min(LocalDateTime::compareTo)
                .orElse(null);
        LocalDateTime endAt = lessons.stream()
                .map(this::lessonEndAt)
                .max(LocalDateTime::compareTo)
                .orElse(null);
        if (startAt == null || endAt == null) {
            return null;
        }
        return new StudentNoticePeriodSummary(startAt, endAt);
    }

    private LocalDateTime lessonStartAt(LessonDto lesson) {
        return LocalDateTime.of(lesson.date(), lesson.startTime());
    }

    private LocalDateTime lessonEndAt(LessonDto lesson) {
        return LocalDateTime.of(lesson.date(), lesson.endTime());
    }
}
