package com.example.interhubdev.offering.internal;

import com.example.interhubdev.offering.GroupSubjectOfferingDto;
import com.example.interhubdev.offering.LessonEnrichmentDataPort;
import com.example.interhubdev.offering.LessonEnrichmentItem;
import com.example.interhubdev.offering.OfferingSlotDto;
import com.example.interhubdev.offering.OfferingSlotKey;
import com.example.interhubdev.offering.OfferingTeacherDto;
import com.example.interhubdev.program.ProgramApi;
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
 * Only depends on repositories; used by adapter for schedule display.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
class OfferingLessonEnrichmentService implements LessonEnrichmentDataPort {

    private final GroupSubjectOfferingRepository offeringRepository;
    private final OfferingSlotRepository slotRepository;
    private final OfferingTeacherRepository offeringTeacherRepository;
    private final ProgramApi programApi;

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
        List<OfferingTeacher> allTeachers = offeringTeacherRepository.findByOfferingIdInOrderByRoleAscCreatedAtAsc(offeringIds.stream().toList());
        Map<UUID, List<OfferingTeacherDto>> teachersByOfferingId = allTeachers.stream()
                .collect(Collectors.groupingBy(OfferingTeacher::getOfferingId,
                        Collectors.mapping(OfferingMappers::toTeacherDto, Collectors.toList())));

        Set<UUID> curriculumSubjectIds = offeringById.values().stream()
                .map(GroupSubjectOfferingDto::curriculumSubjectId)
                .filter(id -> id != null)
                .collect(Collectors.toSet());
        Map<UUID, String> subjectNamesByCurriculumSubjectId = curriculumSubjectIds.isEmpty()
                ? Map.of()
                : programApi.getSubjectNamesByCurriculumSubjectIds(new ArrayList<>(curriculumSubjectIds));

        List<LessonEnrichmentItem> result = new ArrayList<>(keys.size());
        for (OfferingSlotKey key : keys) {
            GroupSubjectOfferingDto offering = offeringById.get(key.offeringId());
            OfferingSlotDto slot = key.slotId() != null ? slotById.get(key.slotId()) : null;
            List<OfferingTeacherDto> teachers = teachersByOfferingId.getOrDefault(key.offeringId(), Collections.emptyList());
            String subjectName = offering != null && offering.curriculumSubjectId() != null
                    ? subjectNamesByCurriculumSubjectId.get(offering.curriculumSubjectId())
                    : null;
            result.add(new LessonEnrichmentItem(offering, slot, teachers, subjectName));
        }
        return result;
    }
}