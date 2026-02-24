package com.example.interhubdev.subject.internal;

import com.example.interhubdev.error.AppException;
import com.example.interhubdev.error.Errors;
import org.springframework.http.HttpStatus;

import java.util.UUID;

/**
 * Module-specific error factory for Subject module.
 * Provides consistent error codes and messages for subject-related errors.
 */
public final class SubjectErrors {
    
    private SubjectErrors() {}
    
    public static final String CODE_TEACHER_PROFILE_NOT_FOUND = "SUBJECT_TEACHER_PROFILE_NOT_FOUND";
    public static final String CODE_CURRICULUM_SUBJECT_NOT_FOUND = "SUBJECT_CURRICULUM_SUBJECT_NOT_FOUND";
    public static final String CODE_ACCESS_DENIED = "SUBJECT_ACCESS_DENIED";
    public static final String CODE_NO_LESSONS_FOR_SLOTS = "SUBJECT_NO_LESSONS_FOR_SLOTS";

    /**
     * No lessons exist for the teacher's offering slots (detail not available).
     */
    public static AppException noLessonsForSlots() {
        return Errors.of(HttpStatus.UNPROCESSABLE_ENTITY, CODE_NO_LESSONS_FOR_SLOTS,
            "По данному предмету нет ни одного занятия по слотам расписания");
    }

    /**
     * Teacher profile not found for user.
     */
    public static AppException teacherProfileNotFound() {
        return Errors.of(HttpStatus.NOT_FOUND, CODE_TEACHER_PROFILE_NOT_FOUND,
            "Преподавательский профиль не найден");
    }
    
    /**
     * Curriculum subject not found.
     */
    public static AppException curriculumSubjectNotFound(UUID id) {
        return Errors.of(HttpStatus.NOT_FOUND, CODE_CURRICULUM_SUBJECT_NOT_FOUND,
            "Предмет учебного плана не найден: " + id);
    }
    
    /**
     * Access denied to curriculum subject.
     */
    public static AppException accessDenied() {
        return Errors.forbidden("Нет доступа к этому предмету");
    }
}
