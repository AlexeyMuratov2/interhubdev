package com.example.interhubdev.offering.internal;

import com.example.interhubdev.error.Errors;
import com.example.interhubdev.group.GroupApi;
import com.example.interhubdev.offering.GroupSubjectOfferingDto;
import com.example.interhubdev.offering.OfferingExistsPort;
import com.example.interhubdev.offering.RoomLookupPort;
import com.example.interhubdev.program.ProgramApi;
import com.example.interhubdev.teacher.TeacherApi;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
class OfferingCatalogService implements OfferingExistsPort {

    private final GroupSubjectOfferingRepository offeringRepository;
    private final GroupApi groupApi;
    private final ProgramApi programApi;
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
        if (groupApi.findGroupById(groupId).isEmpty()) {
            throw Errors.notFound("Group not found: " + groupId);
        }
        if (programApi.findCurriculumSubjectById(curriculumSubjectId).isEmpty()) {
            throw Errors.notFound("Curriculum subject not found: " + curriculumSubjectId);
        }
        if (teacherId != null && teacherApi.findById(teacherId).isEmpty()) {
            throw Errors.notFound("Teacher not found: " + teacherId);
        }
        if (roomId != null && !roomLookupPort.existsById(roomId)) {
            throw Errors.notFound("Room not found: " + roomId);
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
                .orElseThrow(() -> Errors.notFound("Offering not found: " + id));
        if (teacherId != null && teacherApi.findById(teacherId).isEmpty()) {
            throw Errors.notFound("Teacher not found: " + teacherId);
        }
        if (roomId != null && !roomLookupPort.existsById(roomId)) {
            throw Errors.notFound("Room not found: " + roomId);
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
            throw Errors.notFound("Offering not found: " + id);
        }
        offeringRepository.deleteById(id);
    }
}
