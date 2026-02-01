package com.example.interhubdev.offering.internal;

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
        if (groupApi.findGroupById(groupId).isEmpty()) {
            throw new IllegalArgumentException("Group not found: " + groupId);
        }
        if (programApi.findCurriculumSubjectById(curriculumSubjectId).isEmpty()) {
            throw new IllegalArgumentException("Curriculum subject not found: " + curriculumSubjectId);
        }
        if (teacherId != null && teacherApi.findById(teacherId).isEmpty()) {
            throw new IllegalArgumentException("Teacher not found: " + teacherId);
        }
        if (roomId != null && scheduleApi.findRoomById(roomId).isEmpty()) {
            throw new IllegalArgumentException("Room not found: " + roomId);
        }
        if (format != null && !List.of("offline", "online", "mixed").contains(format)) {
            throw new IllegalArgumentException("Format must be offline, online, or mixed");
        }
        if (offeringRepository.existsByGroupIdAndCurriculumSubjectId(groupId, curriculumSubjectId)) {
            throw new IllegalArgumentException("Offering already exists for group and curriculum subject");
        }
        GroupSubjectOffering entity = GroupSubjectOffering.builder()
                .groupId(groupId)
                .curriculumSubjectId(curriculumSubjectId)
                .teacherId(teacherId)
                .roomId(roomId)
                .format(format)
                .notes(notes)
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
                .orElseThrow(() -> new IllegalArgumentException("Offering not found: " + id));
        if (teacherId != null && teacherApi.findById(teacherId).isEmpty()) {
            throw new IllegalArgumentException("Teacher not found: " + teacherId);
        }
        if (roomId != null && scheduleApi.findRoomById(roomId).isEmpty()) {
            throw new IllegalArgumentException("Room not found: " + roomId);
        }
        if (format != null && !List.of("offline", "online", "mixed").contains(format)) {
            throw new IllegalArgumentException("Format must be offline, online, or mixed");
        }
        entity.setTeacherId(teacherId);
        entity.setRoomId(roomId);
        entity.setFormat(format);
        if (notes != null) entity.setNotes(notes);
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
        if (offeringRepository.findById(offeringId).isEmpty()) {
            throw new IllegalArgumentException("Offering not found: " + offeringId);
        }
        if (teacherApi.findById(teacherId).isEmpty()) {
            throw new IllegalArgumentException("Teacher not found: " + teacherId);
        }
        if (!List.of("LECTURE", "PRACTICE", "LAB").contains(role)) {
            throw new IllegalArgumentException("Role must be LECTURE, PRACTICE, or LAB");
        }
        if (offeringTeacherRepository.existsByOfferingIdAndTeacherIdAndRole(offeringId, teacherId, role)) {
            throw new IllegalArgumentException("Offering teacher with this role already exists");
        }
        OfferingTeacher entity = OfferingTeacher.builder()
                .offeringId(offeringId)
                .teacherId(teacherId)
                .role(role)
                .build();
        return toTeacherDto(offeringTeacherRepository.save(entity));
    }

    @Override
    @Transactional
    public void removeOfferingTeacher(UUID id) {
        if (!offeringTeacherRepository.existsById(id)) {
            throw new IllegalArgumentException("Offering teacher not found: " + id);
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
