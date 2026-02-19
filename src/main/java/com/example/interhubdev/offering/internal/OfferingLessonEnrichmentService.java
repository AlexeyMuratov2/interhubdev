package com.example.interhubdev.offering.internal;

import com.example.interhubdev.offering.CurriculumSubjectLookupPort;
import com.example.interhubdev.offering.GroupSubjectOfferingDto;
import com.example.interhubdev.offering.LessonEnrichmentDataPort;
import com.example.interhubdev.offering.LessonEnrichmentItem;
import com.example.interhubdev.offering.OfferingSlotDto;
import com.example.interhubdev.offering.OfferingSlotKey;
import com.example.interhubdev.offering.OfferingTeacherItemDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Batch loads offering, slot and teachers for lesson enrichment (no N+1).
 * Teachers are derived from main teacher and slot teachers per offering.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
class OfferingLessonEnrichmentService implements LessonEnrichmentDataPort {

    private final GroupSubjectOfferingRepository offeringRepository;
    private final OfferingSlotRepository slotRepository;
    private final CurriculumSubjectLookupPort curriculumSubjectLookupPort;

    @Override
    public List<LessonEnrichmentItem> getEnrichmentBatch(List<OfferingSlotKey> keys) {
        if (keys == null || keys.isEmpty()) {
            return List.of();
        }
        Set<UUID> offeringIds = keys.stream().map(OfferingSlotKey::offeringId).collect(Collectors.toSet());
        List<UUID> slotIds = keys.stream().map(OfferingSlotKey::slotId).filter(id -> id != null).distinct().toList();

        Map<UUID, GroupSubjectOfferingDto> offeringById = offeringRepository.findAllById(offeringIds).stream()
                .collect(Collectors.toMap(GroupSubjectOffering::getId, OfferingMappers::toOfferingDto));
        Map<UUID, OfferingSlotDto> slotById = slotIds.isEmpty() ? Map.of() : slotRepository.findAllById(slotIds).stream()
                .collect(Collectors.toMap(OfferingSlot::getId, OfferingMappers::toSlotDto));
        List<OfferingSlot> allSlots = slotRepository.findByOfferingIdInOrderByDayOfWeekAscStartTimeAsc(offeringIds.stream().toList());
        Map<UUID, List<OfferingTeacherItemDto>> teachersByOfferingId = deriveTeachersByOfferingIds(offeringById, allSlots);

        Set<UUID> curriculumSubjectIds = offeringById.values().stream()
                .map(GroupSubjectOfferingDto::curriculumSubjectId)
                .filter(id -> id != null)
                .collect(Collectors.toSet());
        Map<UUID, String> subjectNamesByCurriculumSubjectId = curriculumSubjectIds.isEmpty()
                ? Map.of()
                : curriculumSubjectLookupPort.getSubjectNamesByCurriculumSubjectIds(new ArrayList<>(curriculumSubjectIds));

        List<LessonEnrichmentItem> result = new ArrayList<>(keys.size());
        for (OfferingSlotKey key : keys) {
            GroupSubjectOfferingDto offering = offeringById.get(key.offeringId());
            OfferingSlotDto slot = key.slotId() != null ? slotById.get(key.slotId()) : null;
            List<OfferingTeacherItemDto> teachers = teachersByOfferingId.getOrDefault(key.offeringId(), Collections.emptyList());
            String subjectName = offering != null && offering.curriculumSubjectId() != null
                    ? subjectNamesByCurriculumSubjectId.get(offering.curriculumSubjectId())
                    : null;
            result.add(new LessonEnrichmentItem(offering, slot, teachers, subjectName));
        }
        return result;
    }

    private Map<UUID, List<OfferingTeacherItemDto>> deriveTeachersByOfferingIds(
            Map<UUID, GroupSubjectOfferingDto> offeringById,
            List<OfferingSlot> allSlots) {
        Map<UUID, List<OfferingSlot>> slotsByOfferingId = allSlots.stream()
                .collect(Collectors.groupingBy(OfferingSlot::getOfferingId));
        return offeringById.keySet().stream()
                .map(offeringId -> {
                    GroupSubjectOfferingDto offering = offeringById.get(offeringId);
                    List<OfferingTeacherItemDto> teachers = new ArrayList<>();
                    if (offering != null && offering.teacherId() != null) {
                        teachers.add(new OfferingTeacherItemDto(offering.teacherId(), null));
                    }
                    for (OfferingSlot slot : slotsByOfferingId.getOrDefault(offeringId, List.of())) {
                        if (slot.getTeacherId() != null) {
                            teachers.add(new OfferingTeacherItemDto(slot.getTeacherId(), slot.getLessonType()));
                        }
                    }
                    return Map.entry(offeringId, teachers);
                })
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }
}