package com.example.interhubdev.subject.internal;

import com.example.interhubdev.error.Errors;
import com.example.interhubdev.subject.AssessmentTypeDto;
import com.example.interhubdev.subject.SubjectApi;
import com.example.interhubdev.subject.SubjectDto;
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
class SubjectServiceImpl implements SubjectApi {

    private final SubjectRepository subjectRepository;
    private final AssessmentTypeRepository assessmentTypeRepository;

    @Override
    public Optional<SubjectDto> findSubjectById(UUID id) {
        return subjectRepository.findById(id).map(this::toSubjectDto);
    }

    @Override
    public Optional<SubjectDto> findSubjectByCode(String code) {
        return subjectRepository.findByCode(code).map(this::toSubjectDto);
    }

    @Override
    public List<SubjectDto> findAllSubjects() {
        return subjectRepository.findAll().stream()
                .map(this::toSubjectDto)
                .toList();
    }

    @Override
    @Transactional
    public SubjectDto createSubject(String code, String name, String description, UUID departmentId) {
        if (code == null || code.isBlank()) {
            throw Errors.badRequest("Subject code is required");
        }
        String trimmedCode = code.trim();
        if (subjectRepository.existsByCode(trimmedCode)) {
            throw Errors.conflict("Subject with code '" + trimmedCode + "' already exists");
        }
        Subject entity = Subject.builder()
                .code(trimmedCode)
                .name(name != null ? name.trim() : "")
                .description(description != null ? description.trim() : null)
                .departmentId(departmentId)
                .build();
        return toSubjectDto(subjectRepository.save(entity));
    }

    @Override
    @Transactional
    public SubjectDto updateSubject(UUID id, String name, String description, UUID departmentId) {
        Subject entity = subjectRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Subject not found: " + id));
        if (name != null) entity.setName(name);
        if (description != null) entity.setDescription(description);
        if (departmentId != null) entity.setDepartmentId(departmentId);
        entity.setUpdatedAt(LocalDateTime.now());
        return toSubjectDto(subjectRepository.save(entity));
    }

    @Override
    @Transactional
    public void deleteSubject(UUID id) {
        if (!subjectRepository.existsById(id)) {
            throw Errors.notFound("Subject not found: " + id);
        }
        subjectRepository.deleteById(id);
    }

    @Override
    public Optional<AssessmentTypeDto> findAssessmentTypeById(UUID id) {
        return assessmentTypeRepository.findById(id).map(this::toAssessmentTypeDto);
    }

    @Override
    public Optional<AssessmentTypeDto> findAssessmentTypeByCode(String code) {
        return assessmentTypeRepository.findByCode(code).map(this::toAssessmentTypeDto);
    }

    @Override
    public List<AssessmentTypeDto> findAllAssessmentTypes() {
        return assessmentTypeRepository.findAll().stream()
                .map(this::toAssessmentTypeDto)
                .toList();
    }

    @Override
    @Transactional
    public AssessmentTypeDto createAssessmentType(String code, String name, Boolean isGraded, Boolean isFinal, Integer sortOrder) {
        if (assessmentTypeRepository.existsByCode(code)) {
            throw new IllegalArgumentException("Assessment type with code " + code + " already exists");
        }
        AssessmentType entity = AssessmentType.builder()
                .code(code)
                .name(name != null ? name : "")
                .isGraded(isGraded != null ? isGraded : true)
                .isFinal(isFinal != null ? isFinal : false)
                .sortOrder(sortOrder != null ? sortOrder : 0)
                .build();
        return toAssessmentTypeDto(assessmentTypeRepository.save(entity));
    }

    @Override
    @Transactional
    public AssessmentTypeDto updateAssessmentType(UUID id, String name, Boolean isGraded, Boolean isFinal, Integer sortOrder) {
        AssessmentType entity = assessmentTypeRepository.findById(id)
                .orElseThrow(() -> Errors.notFound("Assessment type not found: " + id));
        if (name != null) entity.setName(name);
        if (isGraded != null) entity.setIsGraded(isGraded);
        if (isFinal != null) entity.setIsFinal(isFinal);
        if (sortOrder != null) entity.setSortOrder(sortOrder);
        return toAssessmentTypeDto(assessmentTypeRepository.save(entity));
    }

    @Override
    @Transactional
    public void deleteAssessmentType(UUID id) {
        if (!assessmentTypeRepository.existsById(id)) {
            throw Errors.notFound("Assessment type not found: " + id);
        }
        assessmentTypeRepository.deleteById(id);
    }

    private SubjectDto toSubjectDto(Subject e) {
        return new SubjectDto(e.getId(), e.getCode(), e.getName(), e.getDescription(),
                e.getDepartmentId(), e.getCreatedAt(), e.getUpdatedAt());
    }

    private AssessmentTypeDto toAssessmentTypeDto(AssessmentType e) {
        return new AssessmentTypeDto(e.getId(), e.getCode(), e.getName(), 
                e.getIsGraded(), e.getIsFinal(), e.getSortOrder(), e.getCreatedAt());
    }
}
