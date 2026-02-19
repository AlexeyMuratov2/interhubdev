package com.example.interhubdev.adapter;

import com.example.interhubdev.offering.GroupSubjectOfferingDto;
import com.example.interhubdev.offering.LessonEnrichmentDataPort;
import com.example.interhubdev.offering.LessonEnrichmentItem;
import com.example.interhubdev.offering.OfferingSlotDto;
import com.example.interhubdev.offering.OfferingSlotKey;
import com.example.interhubdev.offering.OfferingTeacherItemDto;
import com.example.interhubdev.schedule.LessonEnrichmentData;
import com.example.interhubdev.schedule.LessonEnrichmentPort;
import com.example.interhubdev.schedule.LessonEnrichmentRequest;
import com.example.interhubdev.schedule.OfferingSummaryDto;
import com.example.interhubdev.schedule.SlotSummaryDto;
import com.example.interhubdev.schedule.TeacherRoleDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Implements Schedule's LessonEnrichmentPort using Offering's LessonEnrichmentDataPort.
 * Maps Offering DTOs to Schedule's summary types (no Schedule dependency on Offering types).
 */
@Component
@RequiredArgsConstructor
public class LessonEnrichmentAdapter implements LessonEnrichmentPort {

    private final LessonEnrichmentDataPort lessonEnrichmentDataPort;

    @Override
    public List<LessonEnrichmentData> getEnrichment(List<LessonEnrichmentRequest> requests) {
        if (requests == null || requests.isEmpty()) {
            return List.of();
        }
        List<OfferingSlotKey> keys = requests.stream()
                .map(r -> new OfferingSlotKey(r.offeringId(), r.slotId()))
                .toList();
        List<LessonEnrichmentItem> items = lessonEnrichmentDataPort.getEnrichmentBatch(keys);
        return items.stream()
                .map(this::toEnrichmentData)
                .toList();
    }

    private LessonEnrichmentData toEnrichmentData(LessonEnrichmentItem item) {
        OfferingSummaryDto offering = item.offering() != null ? toOfferingSummary(item.offering()) : null;
        SlotSummaryDto slot = item.slot() != null ? toSlotSummary(item.slot()) : null;
        List<TeacherRoleDto> teachers = item.teachers().stream()
                .map(LessonEnrichmentAdapter::toTeacherRole)
                .collect(Collectors.toList());
        return new LessonEnrichmentData(offering, slot, teachers, item.subjectName());
    }

    private static OfferingSummaryDto toOfferingSummary(GroupSubjectOfferingDto o) {
        return new OfferingSummaryDto(o.id(), o.groupId(), o.curriculumSubjectId(), o.teacherId());
    }

    private static SlotSummaryDto toSlotSummary(OfferingSlotDto s) {
        return new SlotSummaryDto(
                s.id(),
                s.offeringId(),
                s.dayOfWeek(),
                s.startTime(),
                s.endTime(),
                s.timeslotId(),
                s.lessonType(),
                s.roomId(),
                s.teacherId(),
                s.createdAt()
        );
    }

    private static TeacherRoleDto toTeacherRole(OfferingTeacherItemDto t) {
        return new TeacherRoleDto(t.teacherId(), t.role());
    }
}
