package com.example.interhubdev.adapter;

import com.example.interhubdev.offering.OfferingApi;
import com.example.interhubdev.subject.OfferingLookupPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Adapter: implements Subject's OfferingLookupPort using Offering module's OfferingApi.
 * Maps offering module's DTOs to subject module's DTOs.
 */
@Component
@RequiredArgsConstructor
public class SubjectOfferingLookupAdapter implements OfferingLookupPort {

    private final OfferingApi offeringApi;

    @Override
    public List<com.example.interhubdev.subject.GroupSubjectOfferingDto> findOfferingsByTeacherId(UUID teacherId) {
        List<com.example.interhubdev.offering.GroupSubjectOfferingDto> offerings = offeringApi.findOfferingsByTeacherId(teacherId);
        return offerings.stream()
                .map(this::toSubjectDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<com.example.interhubdev.subject.GroupSubjectOfferingDto> findOfferingsByCurriculumSubjectIdAndTeacherId(
            UUID curriculumSubjectId, UUID teacherId) {
        List<com.example.interhubdev.offering.GroupSubjectOfferingDto> allOfferings = offeringApi.findOfferingsByTeacherId(teacherId);
        return allOfferings.stream()
                .filter(offering -> offering.curriculumSubjectId().equals(curriculumSubjectId))
                .map(this::toSubjectDto)
                .collect(Collectors.toList());
    }

    private com.example.interhubdev.subject.GroupSubjectOfferingDto toSubjectDto(com.example.interhubdev.offering.GroupSubjectOfferingDto dto) {
        return new com.example.interhubdev.subject.GroupSubjectOfferingDto(
            dto.id(),
            dto.groupId(),
            dto.curriculumSubjectId(),
            dto.teacherId(),
            dto.roomId(),
            dto.format(),
            dto.notes(),
            dto.createdAt(),
            dto.updatedAt()
        );
    }
}
