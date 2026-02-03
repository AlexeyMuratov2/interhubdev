package com.example.interhubdev.teacher.internal;

import com.example.interhubdev.teacher.CreateTeacherRequest;
import com.example.interhubdev.teacher.TeacherApi;
import com.example.interhubdev.teacher.TeacherDto;
import com.example.interhubdev.user.Role;
import com.example.interhubdev.user.UserApi;
import com.example.interhubdev.user.UserDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Implementation of TeacherApi.
 * Package-private: only accessible within the teacher module.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
class TeacherServiceImpl implements TeacherApi {

    private final TeacherRepository teacherRepository;
    private final UserApi userApi;

    @Override
    public Optional<TeacherDto> findByUserId(UUID userId) {
        return teacherRepository.findByUserId(userId).map(this::toDto);
    }

    @Override
    public Optional<TeacherDto> findByTeacherId(String teacherId) {
        return teacherRepository.findByTeacherId(teacherId).map(this::toDto);
    }

    @Override
    public boolean existsByTeacherId(String teacherId) {
        return teacherRepository.existsByTeacherId(teacherId);
    }

    @Override
    public boolean existsByUserId(UUID userId) {
        return teacherRepository.existsByUserId(userId);
    }

    @Override
    public Optional<TeacherDto> findById(UUID id) {
        return teacherRepository.findById(id).map(this::toDto);
    }

    @Override
    public List<TeacherDto> findAll() {
        return teacherRepository.findAll().stream()
                .map(this::toDto)
                .toList();
    }

    @Override
    public List<TeacherDto> findByFaculty(String faculty) {
        return teacherRepository.findByFaculty(faculty).stream()
                .map(this::toDto)
                .toList();
    }

    @Override
    @Transactional
    public TeacherDto create(UUID userId, CreateTeacherRequest request) {
        // Validate user exists and has TEACHER role
        UserDto user = userApi.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        if (!user.hasRole(Role.TEACHER)) {
            throw new IllegalArgumentException("User must have TEACHER role, but has: " + user.roles());
        }

        // Check if profile already exists
        if (teacherRepository.existsByUserId(userId)) {
            throw new IllegalStateException("Teacher profile already exists for user: " + userId);
        }

        // Check if teacherId is unique
        if (teacherRepository.existsByTeacherId(request.teacherId())) {
            throw new IllegalArgumentException("Teacher with ID " + request.teacherId() + " already exists");
        }

        Teacher teacher = Teacher.builder()
                .userId(userId)
                .teacherId(request.teacherId())
                .faculty(request.faculty())
                .englishName(request.englishName())
                .position(request.position())
                .build();

        return toDto(teacherRepository.save(teacher));
    }

    @Override
    @Transactional
    public TeacherDto update(UUID userId, CreateTeacherRequest request) {
        Teacher teacher = teacherRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("Teacher profile not found for user: " + userId));

        // Check if new teacherId conflicts with existing
        if (request.teacherId() != null && !request.teacherId().equals(teacher.getTeacherId())) {
            if (teacherRepository.existsByTeacherId(request.teacherId())) {
                throw new IllegalArgumentException("Teacher with ID " + request.teacherId() + " already exists");
            }
            teacher.setTeacherId(request.teacherId());
        }

        if (request.faculty() != null) {
            teacher.setFaculty(request.faculty());
        }
        if (request.englishName() != null) {
            teacher.setEnglishName(request.englishName());
        }
        if (request.position() != null) {
            teacher.setPosition(request.position());
        }

        teacher.setUpdatedAt(LocalDateTime.now());

        return toDto(teacherRepository.save(teacher));
    }

    @Override
    @Transactional
    public void delete(UUID userId) {
        Teacher teacher = teacherRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("Teacher profile not found for user: " + userId));

        teacherRepository.delete(teacher);
    }

    private TeacherDto toDto(Teacher teacher) {
        return new TeacherDto(
                teacher.getId(),
                teacher.getUserId(),
                teacher.getTeacherId(),
                teacher.getFaculty(),
                teacher.getEnglishName(),
                teacher.getPosition(),
                teacher.getCreatedAt(),
                teacher.getUpdatedAt()
        );
    }
}
