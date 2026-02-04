package com.example.interhubdev.offering.internal;

import com.example.interhubdev.offering.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
class OfferingServiceImpl implements OfferingApi {

    private final OfferingCatalogService catalogService;
    private final OfferingTeacherService teacherService;

    @Override
    public Optional<GroupSubjectOfferingDto> findOfferingById(UUID id) {
        return catalogService.findById(id);
    }

    @Override
    public List<GroupSubjectOfferingDto> findOfferingsByGroupId(UUID groupId) {
        return catalogService.findByGroupId(groupId);
    }

    @Override
    @Transactional
    public GroupSubjectOfferingDto createOffering(
            UUID groupId,
            UUID curriculumSubjectId,
            UUID teacherId,
            UUID roomId,
            String format,
            String notes
    ) {
        return catalogService.create(groupId, curriculumSubjectId, teacherId, roomId, format, notes);
    }

    @Override
    @Transactional
    public GroupSubjectOfferingDto updateOffering(
            UUID id,
            UUID teacherId,
            UUID roomId,
            String format,
            String notes
    ) {
        return catalogService.update(id, teacherId, roomId, format, notes);
    }

    @Override
    @Transactional
    public void deleteOffering(UUID id) {
        catalogService.delete(id);
    }

    @Override
    public List<OfferingTeacherDto> findTeachersByOfferingId(UUID offeringId) {
        return teacherService.findTeachersByOfferingId(offeringId);
    }

    @Override
    @Transactional
    public OfferingTeacherDto addOfferingTeacher(UUID offeringId, UUID teacherId, String role) {
        return teacherService.add(offeringId, teacherId, role);
    }

    @Override
    @Transactional
    public void removeOfferingTeacher(UUID id) {
        teacherService.remove(id);
    }
}
