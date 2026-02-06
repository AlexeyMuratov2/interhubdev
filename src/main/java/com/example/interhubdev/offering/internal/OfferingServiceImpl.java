package com.example.interhubdev.offering.internal;

import com.example.interhubdev.offering.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Facade implementing OfferingApi: delegates to internal services for
 * offerings, teachers, weekly slots, and lesson generation.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
class OfferingServiceImpl implements OfferingApi {

    private final OfferingCatalogService catalogService;
    private final OfferingTeacherService teacherService;
    private final OfferingSlotService slotService;
    private final LessonGenerationService lessonGenerationService;

    // --- Offering CRUD ---

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

    // --- Offering Teachers ---

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

    // --- Offering Slots ---

    @Override
    public List<OfferingSlotDto> findSlotsByOfferingId(UUID offeringId) {
        return slotService.findByOfferingId(offeringId);
    }

    @Override
    @Transactional
    public OfferingSlotDto addOfferingSlot(UUID offeringId, UUID timeslotId, String lessonType, UUID roomId, UUID teacherId) {
        return slotService.add(offeringId, timeslotId, lessonType, roomId, teacherId);
    }

    @Override
    @Transactional
    public void removeOfferingSlot(UUID id) {
        slotService.remove(id);
    }

    // --- Lesson Generation ---

    @Override
    @Transactional
    public int generateLessonsForOffering(UUID offeringId, UUID semesterId) {
        return lessonGenerationService.generateForOffering(offeringId, semesterId);
    }

    @Override
    @Transactional
    public int generateLessonsForGroup(UUID groupId, UUID semesterId) {
        return lessonGenerationService.generateForGroup(groupId, semesterId);
    }

    @Override
    @Transactional
    public int regenerateLessonsForOffering(UUID offeringId, UUID semesterId) {
        return lessonGenerationService.regenerateForOffering(offeringId, semesterId);
    }
}
