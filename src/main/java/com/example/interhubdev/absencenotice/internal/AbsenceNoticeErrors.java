package com.example.interhubdev.absencenotice.internal;

import com.example.interhubdev.error.AppException;
import com.example.interhubdev.error.Errors;
import org.springframework.http.HttpStatus;

import java.util.UUID;

/**
 * Absence notice module error codes and factories.
 */
public final class AbsenceNoticeErrors {

    private AbsenceNoticeErrors() {
    }

    public static final String CODE_NOTICE_NOT_FOUND = "ATTENDANCE_NOTICE_NOT_FOUND";
    public static final String CODE_NOTICE_NOT_OWNED = "ATTENDANCE_NOTICE_NOT_OWNED";
    public static final String CODE_NOTICE_NOT_CANCELABLE = "ATTENDANCE_NOTICE_NOT_CANCELABLE";
    public static final String CODE_NOTICE_CANNOT_UPDATE_AFTER_RESPONSE = "ATTENDANCE_NOTICE_CANNOT_UPDATE_AFTER_RESPONSE";
    public static final String CODE_NOTICE_ALREADY_EXISTS = "ATTENDANCE_NOTICE_ALREADY_EXISTS";
    public static final String CODE_SESSION_NOT_FOUND = "ATTENDANCE_SESSION_NOT_FOUND";
    public static final String CODE_INVALID_ATTACHMENT_COUNT = "ATTENDANCE_INVALID_ATTACHMENT_COUNT";
    public static final String CODE_INVALID_FILE_ID = "ATTENDANCE_INVALID_FILE_ID";
    public static final String CODE_NOTICE_CANCELED = "ATTENDANCE_NOTICE_CANCELED";
    public static final String CODE_NOTICE_DOES_NOT_MATCH_RECORD = "ATTENDANCE_NOTICE_DOES_NOT_MATCH_RECORD";
    public static final String CODE_RECORD_NOT_FOUND = "ATTENDANCE_RECORD_NOT_FOUND";
    public static final String CODE_OFFERING_NOT_FOUND = "ATTENDANCE_OFFERING_NOT_FOUND";
    public static final String CODE_STUDENT_NOT_IN_GROUP = "ATTENDANCE_STUDENT_NOT_IN_GROUP";
    public static final String CODE_VALIDATION_FAILED = "ATTENDANCE_VALIDATION_FAILED";
    public static final String CODE_FORBIDDEN = "ATTENDANCE_FORBIDDEN";
    public static final String CODE_NOTICE_DOES_NOT_COVER_SESSION = "ATTENDANCE_NOTICE_DOES_NOT_COVER_SESSION";

    public static AppException noticeNotFound(UUID id) {
        return Errors.of(HttpStatus.NOT_FOUND, CODE_NOTICE_NOT_FOUND, "Absence notice not found: " + id);
    }

    public static AppException noticeNotOwned(UUID noticeId) {
        return Errors.of(HttpStatus.FORBIDDEN, CODE_NOTICE_NOT_OWNED,
                "Absence notice " + noticeId + " does not belong to the current student");
    }

    public static AppException noticeNotCancelable(UUID noticeId, String reason) {
        return Errors.of(HttpStatus.BAD_REQUEST, CODE_NOTICE_NOT_CANCELABLE,
                "Absence notice " + noticeId + " cannot be canceled: " + reason);
    }

    public static AppException noticeCannotBeUpdatedAfterResponse(UUID noticeId) {
        return Errors.of(HttpStatus.BAD_REQUEST, CODE_NOTICE_CANNOT_UPDATE_AFTER_RESPONSE,
                "Absence notice " + noticeId + " cannot be updated in current status");
    }

    public static AppException noticeAlreadyExistsForSession(UUID sessionId, UUID studentId) {
        return Errors.of(HttpStatus.CONFLICT, CODE_NOTICE_ALREADY_EXISTS,
                "Student already has an active absence notice for this session. Use update instead.");
    }

    public static AppException sessionNotFound(UUID sessionId) {
        return Errors.of(HttpStatus.NOT_FOUND, CODE_SESSION_NOT_FOUND, "Lesson session not found: " + sessionId);
    }

    public static AppException invalidAttachmentCount(int count, int max) {
        return Errors.of(HttpStatus.BAD_REQUEST, CODE_INVALID_ATTACHMENT_COUNT,
                "Too many attachments: " + count + " (maximum: " + max + ")");
    }

    public static AppException invalidFileId(String fileId, String reason) {
        return Errors.of(HttpStatus.BAD_REQUEST, CODE_INVALID_FILE_ID,
                "Invalid file ID: " + fileId + ". " + reason);
    }

    public static AppException noticeCanceled(UUID noticeId) {
        return Errors.of(HttpStatus.BAD_REQUEST, CODE_NOTICE_CANCELED,
                "Absence notice " + noticeId + " is canceled and cannot be attached");
    }

    public static AppException noticeDoesNotMatchRecord(UUID noticeId, UUID recordId, String reason) {
        return Errors.of(HttpStatus.BAD_REQUEST, CODE_NOTICE_DOES_NOT_MATCH_RECORD,
                "Absence notice " + noticeId + " does not match attendance record " + recordId + ": " + reason);
    }

    public static AppException recordNotFound(UUID id) {
        return Errors.of(HttpStatus.NOT_FOUND, CODE_RECORD_NOT_FOUND, "Attendance record not found: " + id);
    }

    public static AppException offeringNotFound(UUID id) {
        return Errors.of(HttpStatus.NOT_FOUND, CODE_OFFERING_NOT_FOUND, "Offering not found: " + id);
    }

    public static AppException studentNotInGroup(UUID studentId, UUID groupId) {
        return Errors.of(HttpStatus.BAD_REQUEST, CODE_STUDENT_NOT_IN_GROUP,
                "Student " + studentId + " is not in group " + groupId + " roster");
    }

    public static AppException validationFailed(String message) {
        return Errors.of(HttpStatus.BAD_REQUEST, CODE_VALIDATION_FAILED, message);
    }

    public static AppException forbidden(String message) {
        return Errors.of(HttpStatus.FORBIDDEN, CODE_FORBIDDEN, message);
    }

    public static AppException noticeDoesNotCoverSession(UUID noticeId, UUID sessionId) {
        return Errors.of(
                HttpStatus.BAD_REQUEST,
                CODE_NOTICE_DOES_NOT_COVER_SESSION,
                "Absence notice " + noticeId + " does not cover lesson session " + sessionId
        );
    }
}
