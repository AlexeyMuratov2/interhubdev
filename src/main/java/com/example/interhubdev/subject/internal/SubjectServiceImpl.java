package com.example.interhubdev.subject.internal;

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
    public SubjectDto createSubject(String code, String name, String description) {
        if (subjectRepository.existsByCode(code)) {
            throw new IllegalArgumentException("Subject with code " + code + " already exists");
        }
        Subject entity = Subject.builder()
                .code(code)
                .name(name != null ? name : "")
                .description(description)
                .build();
        return toSubjectDto(subjectRepository.save(entity));
    }

    @Override
    @Transactional
    public SubjectDto updateSubject(UUID id, String name, String description) {
        Subject entity = subjectRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Subject not found: " + id));
        if (name != null) entity.setName(name);
        if (description != null) entity.setDescription(description);
        entity.setUpdatedAt(LocalDateTime.now());
        return toSubjectDto(subjectRepository.save(entity));
    }

    @Override
    @Transactional
    public void deleteSubject(UUID id) {
        if (!subjectRepository.existsById(id)) {
            throw new IllegalArgumentException("Subject not found: " + id);
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
    public AssessmentTypeDto createAssessmentType(String code, String name) {
        if (assessmentTypeRepository.existsByCode(code)) {
            throw new IllegalArgumentException("Assessment type with code " + code + " already exists");
        }
        AssessmentType entity = AssessmentType.builder()
                .code(code)
                .name(name != null ? name : "")
                .build();
        return toAssessmentTypeDto(assessmentTypeRepository.save(entity));
    }

    @Override
    @Transactional
    public void deleteAssessmentType(UUID id) {
        if (!assessmentTypeRepository.existsById(id)) {
            throw new IllegalArgumentException("Assessment type not found: " + id);
        }
        assessmentTypeRepository.deleteById(id);
    }

    private SubjectDto toSubjectDto(Subject e) {
        return new SubjectDto(e.getId(), e.getCode(), e.getName(), e.getDescription(),
                e.getCreatedAt(), e.getUpdatedAt());
    }

    private AssessmentTypeDto toAssessmentTypeDto(AssessmentType e) {
        return new AssessmentTypeDto(e.getId(), e.getCode(), e.getName(), e.getCreatedAt());
    }
}
