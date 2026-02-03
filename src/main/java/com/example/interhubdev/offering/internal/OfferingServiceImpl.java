package com.example.interhubdev.offering.internal;

import com.example.interhubdev.error.Errors;
import com.example.interhubdev.group.GroupApi;
import com.example.interhubdev.offering.*;
import com.example.interhubdev.program.ProgramApi;
import com.example.interhubdev.schedule.ScheduleApi;
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
class OfferingServiceImpl implements OfferingApi {

    private static final List<String> VALID_FORMATS = List.of("offline", "online", "mixed");
    private static final List<String> VALID_TEACHER_ROLES = List.of("LECTURE", "PRACTICE", "LAB");

    private final GroupSubjectOfferingRepository offeringRepository;
    private final OfferingTeacherRepository offeringTeacherRepository;
    private final GroupApi groupApi;
    private final ProgramApi programApi;
    private final TeacherApi teacherApi;
    private final ScheduleApi scheduleApi;

    @Override
    public Optional<GroupSubjectOfferingDto> findOfferingById(UUID id) {
        return offeringRepository.findById(id).map(this::toOfferingDto);
    }

    @Override
    public List<GroupSubjectOfferingDto> findOfferingsByGroupId(UUID groupId) {
        return offeringRepository.findByGroupId(groupId).stream()
                .map(this::toOfferingDto)
                .toList();
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
        if (roomId != null && scheduleApi.findRoomById(roomId).isEmpty()) {
            throw Errors.notFound("Room not found: " + roomId);
        }
        String normalizedFormat = format != null ? format.trim().toLowerCase() : null;
        if (normalizedFormat != null && !VALID_FORMATS.contains(normalizedFormat)) {
            throw Errors.badRequest("Format must be offline, online, or mixed");
        }
        if (offeringRepository.existsByGroupIdAndCurriculumSubjectId(groupId, curriculumSubjectId)) {
            throw Errors.conflict("Offering already exists for group and curriculum subject");
        }
        GroupSubjectOffering entity = GroupSubjectOffering.builder()
                .groupId(groupId)
                .curriculumSubjectId(curriculumSubjectId)
                .teacherId(teacherId)
                .roomId(roomId)
                .format(normalizedFormat)
                .notes(notes != null ? notes.trim() : null)
                .build();
        return toOfferingDto(offeringRepository.save(entity));
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
        GroupSubjectOffering entity = offeringRepository.findById(id)
                .orElseThrow(() -> Errors.notFound("Offering not found: " + id));
        if (teacherId != null && teacherApi.findById(teacherId).isEmpty()) {
            throw Errors.notFound("Teacher not found: " + teacherId);
        }
        if (roomId != null && scheduleApi.findRoomById(roomId).isEmpty()) {
            throw Errors.notFound("Room not found: " + roomId);
        }
        String normalizedFormat = format != null ? format.trim().toLowerCase() : null;
        if (normalizedFormat != null && !VALID_FORMATS.contains(normalizedFormat)) {
            throw Errors.badRequest("Format must be offline, online, or mixed");
        }
        entity.setTeacherId(teacherId);
        entity.setRoomId(roomId);
        entity.setFormat(normalizedFormat);
        if (notes != null) entity.setNotes(notes.trim());
        entity.setUpdatedAt(LocalDateTime.now());
        return toOfferingDto(offeringRepository.save(entity));
    }

    @Override
    @Transactional
    public void deleteOffering(UUID id) {
        if (!offeringRepository.existsById(id)) {
            throw new IllegalArgumentException("Offering not found: " + id);
        }
        offeringRepository.deleteById(id);
    }

    @Override
    public List<OfferingTeacherDto> findTeachersByOfferingId(UUID offeringId) {
        return offeringTeacherRepository.findByOfferingId(offeringId).stream()
                .map(this::toTeacherDto)
                .toList();
    }

    @Override
    @Transactional
    public OfferingTeacherDto addOfferingTeacher(UUID offeringId, UUID teacherId, String role) {
        if (teacherId == null) {
            throw Errors.badRequest("Teacher id is required");
        }
        if (role == null || role.isBlank()) {
            throw Errors.badRequest("Role is required");
        }
        if (offeringRepository.findById(offeringId).isEmpty()) {
            throw Errors.notFound("Offering not found: " + offeringId);
        }
        if (teacherApi.findById(teacherId).isEmpty()) {
            throw Errors.notFound("Teacher not found: " + teacherId);
        }
        String normalizedRole = role.trim().toUpperCase();
        if (!VALID_TEACHER_ROLES.contains(normalizedRole)) {
            throw Errors.badRequest("Role must be LECTURE, PRACTICE, or LAB");
        }
        if (offeringTeacherRepository.existsByOfferingIdAndTeacherIdAndRole(offeringId, teacherId, normalizedRole)) {
            throw Errors.conflict("Offering teacher with this role already exists");
        }
        OfferingTeacher entity = OfferingTeacher.builder()
                .offeringId(offeringId)
                .teacherId(teacherId)
                .role(normalizedRole)
                .build();
        return toTeacherDto(offeringTeacherRepository.save(entity));
    }

    @Override
    @Transactional
    public void removeOfferingTeacher(UUID id) {
        if (!offeringTeacherRepository.existsById(id)) {
            throw Errors.notFound("Offering teacher not found: " + id);
        }
        offeringTeacherRepository.deleteById(id);
    }

    private GroupSubjectOfferingDto toOfferingDto(GroupSubjectOffering e) {
        return new GroupSubjectOfferingDto(e.getId(), e.getGroupId(), e.getCurriculumSubjectId(),
                e.getTeacherId(), e.getRoomId(), e.getFormat(), e.getNotes(), e.getCreatedAt(), e.getUpdatedAt());
    }

    private OfferingTeacherDto toTeacherDto(OfferingTeacher e) {
        return new OfferingTeacherDto(e.getId(), e.getOfferingId(), e.getTeacherId(), e.getRole(), e.getCreatedAt());
    }
}
