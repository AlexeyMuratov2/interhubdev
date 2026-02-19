package com.example.interhubdev.offering.internal;

import com.example.interhubdev.error.Errors;
import com.example.interhubdev.offering.CurriculumSubjectLookupPort;
import com.example.interhubdev.offering.GroupLookupPort;
import com.example.interhubdev.offering.GroupSubjectOfferingDto;
import com.example.interhubdev.offering.OfferingExistsPort;
import com.example.interhubdev.offering.OfferingTeacherItemDto;
import com.example.interhubdev.offering.RoomLookupPort;
import com.example.interhubdev.teacher.TeacherApi;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
class OfferingCatalogService implements OfferingExistsPort {

    private final GroupSubjectOfferingRepository offeringRepository;
    private final OfferingSlotRepository slotRepository;
    private final GroupLookupPort groupLookupPort;
    private final CurriculumSubjectLookupPort curriculumSubjectLookupPort;
    private final TeacherApi teacherApi;
    private final RoomLookupPort roomLookupPort;

    @Override
    public boolean existsById(UUID offeringId) {
        return offeringRepository.existsById(offeringId);
    }

    Optional<GroupSubjectOfferingDto> findById(UUID id) {
        return offeringRepository.findById(id).map(OfferingMappers::toOfferingDto);
    }

    List<GroupSubjectOfferingDto> findByGroupId(UUID groupId) {
        return offeringRepository.findByGroupIdOrderByCurriculumSubjectIdAsc(groupId).stream()
                .map(OfferingMappers::toOfferingDto)
                .toList();
    }

    /**
     * Derive list of teachers for an offering from main teacher and slot teachers.
     * Main teacher has role null; slot teachers have role = slot's lessonType (LECTURE, PRACTICE, LAB).
     */
    List<OfferingTeacherItemDto> deriveTeachersByOfferingId(UUID offeringId) {
        GroupSubjectOffering offering = offeringRepository.findById(offeringId)
                .orElseThrow(() -> OfferingErrors.offeringNotFound(offeringId));
        List<OfferingTeacherItemDto> result = new ArrayList<>();
        if (offering.getTeacherId() != null) {
            result.add(new OfferingTeacherItemDto(offering.getTeacherId(), null));
        }
        List<OfferingSlot> slots = slotRepository.findByOfferingIdOrderByDayOfWeekAscStartTimeAsc(offeringId);
        for (OfferingSlot slot : slots) {
            if (slot.getTeacherId() != null) {
                result.add(new OfferingTeacherItemDto(slot.getTeacherId(), slot.getLessonType()));
            }
        }
        return result;
    }

    List<GroupSubjectOfferingDto> findByTeacherId(UUID teacherId) {
        java.util.Set<UUID> offeringIds = new java.util.HashSet<>();
        
        // Offerings where teacher is the main teacher
        offeringIds.addAll(offeringRepository.findByTeacherId(teacherId).stream()
                .map(GroupSubjectOffering::getId)
                .collect(java.util.stream.Collectors.toSet()));
        
        // Offerings where teacher is assigned to a slot
        offeringIds.addAll(slotRepository.findByTeacherId(teacherId).stream()
                .map(OfferingSlot::getOfferingId)
                .collect(java.util.stream.Collectors.toSet()));
        
        return offeringIds.stream()
                .map(offeringRepository::findById)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .map(OfferingMappers::toOfferingDto)
                .toList();
    }

    @Transactional
    GroupSubjectOfferingDto create(
            UUID groupId,
            UUID curriculumSubjectId,
            UUID teacherId,
            UUID roomId,
            String format,
            String notes
    ) {
        if (groupId == null) {
            throw Errors.badRequest("Group id is required");
        }
        if (curriculumSubjectId == null) {
            throw Errors.badRequest("Curriculum subject id is required");
        }
        if (!groupLookupPort.existsById(groupId)) {
            throw Errors.notFound("Group not found");
        }
        if (curriculumSubjectLookupPort.findById(curriculumSubjectId).isEmpty()) {
            throw Errors.notFound("Curriculum subject not found");
        }
        if (teacherId != null && teacherApi.findById(teacherId).isEmpty()) {
            throw Errors.notFound("Teacher not found");
        }
        if (roomId != null && !roomLookupPort.existsById(roomId)) {
            throw Errors.notFound("Room not found");
        }
        String normalizedFormat = (format == null || format.isBlank()) ? null : OfferingValidation.normalizeFormat(format);
        if (offeringRepository.existsByGroupIdAndCurriculumSubjectId(groupId, curriculumSubjectId)) {
            throw Errors.conflict("Offering already exists for group and curriculum subject");
        }
        GroupSubjectOffering entity = GroupSubjectOffering.builder()
                .groupId(groupId)
                .curriculumSubjectId(curriculumSubjectId)
                .teacherId(teacherId)
                .roomId(roomId)
                .format(normalizedFormat)
                .notes(OfferingValidation.trimNotes(notes))
                .build();
        return OfferingMappers.toOfferingDto(offeringRepository.save(entity));
    }

    @Transactional
    GroupSubjectOfferingDto update(UUID id, UUID teacherId, UUID roomId, String format, String notes) {
        GroupSubjectOffering entity = offeringRepository.findById(id)
                .orElseThrow(() -> OfferingErrors.offeringNotFound(id));
        if (teacherId != null && teacherApi.findById(teacherId).isEmpty()) {
            throw Errors.notFound("Teacher not found");
        }
        if (roomId != null && !roomLookupPort.existsById(roomId)) {
            throw Errors.notFound("Room not found");
        }
        String normalizedFormat = format != null && !format.isBlank() ? OfferingValidation.normalizeFormat(format) : null;
        entity.setTeacherId(teacherId);
        entity.setRoomId(roomId);
        entity.setFormat(normalizedFormat);
        if (notes != null) {
            entity.setNotes(notes.trim());
        }
        entity.setUpdatedAt(LocalDateTime.now());
        return OfferingMappers.toOfferingDto(offeringRepository.save(entity));
    }

    @Transactional
    void delete(UUID id) {
        if (!offeringRepository.existsById(id)) {
            throw OfferingErrors.offeringNotFound(id);
        }
        slotRepository.deleteByOfferingId(id);
        offeringRepository.deleteById(id);
    }
}
