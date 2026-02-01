package com.example.interhubdev.student.internal;

import com.example.interhubdev.student.CreateStudentRequest;
import com.example.interhubdev.student.StudentApi;
import com.example.interhubdev.student.StudentDto;
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
 * Implementation of StudentApi.
 * Package-private: only accessible within the student module.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
class StudentServiceImpl implements StudentApi {

    private final StudentRepository studentRepository;
    private final UserApi userApi;

    @Override
    public Optional<StudentDto> findById(UUID id) {
        return studentRepository.findById(id).map(this::toDto);
    }

    @Override
    public Optional<StudentDto> findByUserId(UUID userId) {
        return studentRepository.findByUserId(userId).map(this::toDto);
    }

    @Override
    public Optional<StudentDto> findByStudentId(String studentId) {
        return studentRepository.findByStudentId(studentId).map(this::toDto);
    }

    @Override
    public boolean existsByStudentId(String studentId) {
        return studentRepository.existsByStudentId(studentId);
    }

    @Override
    public boolean existsByUserId(UUID userId) {
        return studentRepository.existsByUserId(userId);
    }

    @Override
    public List<StudentDto> findAll() {
        return studentRepository.findAll().stream()
                .map(this::toDto)
                .toList();
    }

    @Override
    public List<StudentDto> findByFaculty(String faculty) {
        return studentRepository.findByFaculty(faculty).stream()
                .map(this::toDto)
                .toList();
    }

    @Override
    public List<StudentDto> findByGroupName(String groupName) {
        return studentRepository.findByGroupName(groupName).stream()
                .map(this::toDto)
                .toList();
    }

    @Override
    public List<StudentDto> findByGroupId(UUID groupId) {
        return studentRepository.findByGroupId(groupId).stream()
                .map(this::toDto)
                .toList();
    }

    @Override
    @Transactional
    public StudentDto updateGroupId(UUID userId, UUID groupId) {
        Student student = studentRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("Student profile not found for user: " + userId));
        student.setGroupId(groupId);
        student.setUpdatedAt(LocalDateTime.now());
        return toDto(studentRepository.save(student));
    }

    @Override
    @Transactional
    public StudentDto create(UUID userId, CreateStudentRequest request) {
        // Validate user exists and has STUDENT role
        UserDto user = userApi.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        if (user.role() != Role.STUDENT) {
            throw new IllegalArgumentException("User must have STUDENT role, but has: " + user.role());
        }

        // Check if profile already exists
        if (studentRepository.existsByUserId(userId)) {
            throw new IllegalStateException("Student profile already exists for user: " + userId);
        }

        // Check if studentId is unique
        if (studentRepository.existsByStudentId(request.studentId())) {
            throw new IllegalArgumentException("Student with ID " + request.studentId() + " already exists");
        }

        Student student = Student.builder()
                .userId(userId)
                .studentId(request.studentId())
                .chineseName(request.chineseName())
                .faculty(request.faculty())
                .course(request.course())
                .enrollmentYear(request.enrollmentYear())
                .groupName(request.groupName())
                .build();

        return toDto(studentRepository.save(student));
    }

    @Override
    @Transactional
    public StudentDto update(UUID userId, CreateStudentRequest request) {
        Student student = studentRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("Student profile not found for user: " + userId));

        // Check if new studentId conflicts with existing
        if (request.studentId() != null && !request.studentId().equals(student.getStudentId())) {
            if (studentRepository.existsByStudentId(request.studentId())) {
                throw new IllegalArgumentException("Student with ID " + request.studentId() + " already exists");
            }
            student.setStudentId(request.studentId());
        }

        if (request.chineseName() != null) {
            student.setChineseName(request.chineseName());
        }
        if (request.faculty() != null) {
            student.setFaculty(request.faculty());
        }
        if (request.course() != null) {
            student.setCourse(request.course());
        }
        if (request.enrollmentYear() != null) {
            student.setEnrollmentYear(request.enrollmentYear());
        }
        if (request.groupName() != null) {
            student.setGroupName(request.groupName());
        }

        student.setUpdatedAt(LocalDateTime.now());

        return toDto(studentRepository.save(student));
    }

    @Override
    @Transactional
    public void delete(UUID userId) {
        Student student = studentRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("Student profile not found for user: " + userId));

        studentRepository.delete(student);
    }

    private StudentDto toDto(Student student) {
        return new StudentDto(
                student.getId(),
                student.getUserId(),
                student.getStudentId(),
                student.getChineseName(),
                student.getFaculty(),
                student.getCourse(),
                student.getEnrollmentYear(),
                student.getGroupName(),
                student.getGroupId(),
                student.getCreatedAt(),
                student.getUpdatedAt()
        );
    }
}
