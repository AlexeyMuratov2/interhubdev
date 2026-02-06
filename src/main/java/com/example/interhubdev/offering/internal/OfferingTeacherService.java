package com.example.interhubdev.offering.internal;

import com.example.interhubdev.error.Errors;
import com.example.interhubdev.offering.OfferingTeacherDto;
import com.example.interhubdev.teacher.TeacherApi;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
class OfferingTeacherService {

    private final OfferingTeacherRepository offeringTeacherRepository;
    private final GroupSubjectOfferingRepository offeringRepository;
    private final TeacherApi teacherApi;

    List<OfferingTeacherDto> findTeachersByOfferingId(UUID offeringId) {
        return offeringTeacherRepository.findByOfferingIdOrderByRoleAscCreatedAtAsc(offeringId).stream()
                .map(OfferingMappers::toTeacherDto)
                .toList();
    }

    @Transactional
    OfferingTeacherDto add(UUID offeringId, UUID teacherId, String role) {
        if (teacherId == null) {
            throw Errors.badRequest("Teacher id is required");
        }
        if (offeringRepository.findById(offeringId).isEmpty()) {
            throw Errors.notFound("Offering not found");
        }
        if (teacherApi.findById(teacherId).isEmpty()) {
            throw Errors.notFound("Teacher not found");
        }
        String normalizedRole = OfferingValidation.normalizeRole(role);
        if (offeringTeacherRepository.existsByOfferingIdAndTeacherIdAndRole(offeringId, teacherId, normalizedRole)) {
            throw Errors.conflict("Offering teacher with this role already exists");
        }
        OfferingTeacher entity = OfferingTeacher.builder()
                .offeringId(offeringId)
                .teacherId(teacherId)
                .role(normalizedRole)
                .build();
        return OfferingMappers.toTeacherDto(offeringTeacherRepository.save(entity));
    }

    @Transactional
    void remove(UUID id) {
        if (!offeringTeacherRepository.existsById(id)) {
            throw Errors.notFound("Offering teacher not found");
        }
        offeringTeacherRepository.deleteById(id);
    }
}
