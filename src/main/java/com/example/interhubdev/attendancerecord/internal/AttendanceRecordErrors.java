package com.example.interhubdev.attendancerecord.internal;

import com.example.interhubdev.error.AppException;
import com.example.interhubdev.error.Errors;
import org.springframework.http.HttpStatus;

import java.util.UUID;

/**
 * Attendance record module error codes and factories.
 */
public final class AttendanceRecordErrors {

    private AttendanceRecordErrors() {
    }

    public static final String CODE_LESSON_NOT_FOUND = "ATTENDANCE_LESSON_NOT_FOUND";
    public static final String CODE_STUDENT_NOT_FOUND = "ATTENDANCE_STUDENT_NOT_FOUND";
    public static final String CODE_GROUP_NOT_FOUND = "ATTENDANCE_GROUP_NOT_FOUND";
    public static final String CODE_OFFERING_NOT_FOUND = "ATTENDANCE_OFFERING_NOT_FOUND";
    public static final String CODE_FORBIDDEN = "ATTENDANCE_FORBIDDEN";
    public static final String CODE_STUDENT_NOT_IN_GROUP = "ATTENDANCE_STUDENT_NOT_IN_GROUP";
    public static final String CODE_VALIDATION_FAILED = "ATTENDANCE_VALIDATION_FAILED";
    public static final String CODE_RECORD_NOT_FOUND = "ATTENDANCE_RECORD_NOT_FOUND";

    public static AppException lessonNotFound(UUID id) {
        return Errors.of(HttpStatus.NOT_FOUND, CODE_LESSON_NOT_FOUND, "Lesson session not found: " + id);
    }

    public static AppException studentNotFound(UUID id) {
        return Errors.of(HttpStatus.NOT_FOUND, CODE_STUDENT_NOT_FOUND, "Student not found: " + id);
    }

    public static AppException groupNotFound(UUID id) {
        return Errors.of(HttpStatus.NOT_FOUND, CODE_GROUP_NOT_FOUND, "Group not found: " + id);
    }

    public static AppException offeringNotFound(UUID id) {
        return Errors.of(HttpStatus.NOT_FOUND, CODE_OFFERING_NOT_FOUND, "Offering not found: " + id);
    }

    public static AppException forbidden(String message) {
        return Errors.of(HttpStatus.FORBIDDEN, CODE_FORBIDDEN, message);
    }

    public static AppException studentNotInGroup(UUID studentId, UUID groupId) {
        return Errors.of(HttpStatus.BAD_REQUEST, CODE_STUDENT_NOT_IN_GROUP,
                "Student " + studentId + " is not in group " + groupId + " roster");
    }

    public static AppException validationFailed(String message) {
        return Errors.of(HttpStatus.BAD_REQUEST, CODE_VALIDATION_FAILED, message);
    }

    public static AppException recordNotFound(UUID id) {
        return Errors.of(HttpStatus.NOT_FOUND, CODE_RECORD_NOT_FOUND, "Attendance record not found: " + id);
    }
}
