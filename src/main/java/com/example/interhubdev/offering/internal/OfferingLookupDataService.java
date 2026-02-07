package com.example.interhubdev.offering.internal;

import com.example.interhubdev.offering.OfferingLookupDataPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Minimal implementation of OfferingLookupDataPort using only the offering repository.
 * No dependency on Schedule or other modules that could create a circular dependency.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
class OfferingLookupDataService implements OfferingLookupDataPort {

    private final GroupSubjectOfferingRepository offeringRepository;

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
}
