package com.example.interhubdev.offering.internal;

import com.example.interhubdev.offering.OfferingLookupDataPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Minimal implementation of OfferingLookupDataPort using only the offering repositories.
 * No dependency on Schedule or other modules that could create a circular dependency.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
class OfferingLookupDataService implements OfferingLookupDataPort {

    private final GroupSubjectOfferingRepository offeringRepository;
    private final OfferingSlotRepository slotRepository;
    private final OfferingTeacherRepository offeringTeacherRepository;

    @Override
    public boolean existsById(UUID offeringId) {
        return offeringRepository.existsById(offeringId);
    }

    @Override
    public List<UUID> findOfferingIdsByGroupId(UUID groupId) {
        return offeringRepository.findByGroupIdOrderByCurriculumSubjectIdAsc(groupId).stream()
                .map(GroupSubjectOffering::getId)
                .toList();
    }

    @Override
    public List<UUID> findOfferingIdsByTeacherId(UUID teacherId) {
        Set<UUID> offeringIds = new HashSet<>();
        
        // Offerings where teacher is the main teacher
        offeringIds.addAll(offeringRepository.findByTeacherId(teacherId).stream()
                .map(GroupSubjectOffering::getId)
                .collect(Collectors.toSet()));
        
        // Offerings where teacher is assigned to a slot
        offeringIds.addAll(slotRepository.findByTeacherId(teacherId).stream()
                .map(OfferingSlot::getOfferingId)
                .collect(Collectors.toSet()));
        
        // Offerings where teacher is assigned as offering teacher
        offeringIds.addAll(offeringTeacherRepository.findByTeacherId(teacherId).stream()
                .map(OfferingTeacher::getOfferingId)
                .collect(Collectors.toSet()));
        
        return offeringIds.stream().toList();
    }
}
