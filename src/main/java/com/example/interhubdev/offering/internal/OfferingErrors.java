package com.example.interhubdev.offering.internal;

import com.example.interhubdev.error.AppException;
import com.example.interhubdev.error.Errors;
import org.springframework.http.HttpStatus;

import java.util.UUID;

/**
 * Offering module error codes and user-facing messages.
 * All errors are thrown as {@link AppException} and handled by global handler.
 */
public final class OfferingErrors {

    private OfferingErrors() {
    }

    /** Offering has no weekly slots assigned — cannot generate lessons. */
    public static final String CODE_NO_SLOTS = "OFFERING_NO_SLOTS";
    /** Lessons already exist for this offering — use regenerate instead. */
    public static final String CODE_LESSONS_ALREADY_EXIST = "OFFERING_LESSONS_ALREADY_EXIST";
    /** Semester not found when generating lessons. */
    public static final String CODE_SEMESTER_NOT_FOUND = "OFFERING_SEMESTER_NOT_FOUND";
    /** Curriculum subject not found when generating lessons. */
    public static final String CODE_CURRICULUM_SUBJECT_NOT_FOUND = "OFFERING_CURRICULUM_SUBJECT_NOT_FOUND";
    /** Offering not found. */
    public static final String CODE_OFFERING_NOT_FOUND = "OFFERING_NOT_FOUND";
    /** Timeslot info could not be resolved for a slot. */
    public static final String CODE_TIMESLOT_NOT_RESOLVED = "OFFERING_TIMESLOT_NOT_RESOLVED";

    public static AppException noSlots(UUID offeringId) {
        return Errors.of(HttpStatus.BAD_REQUEST, CODE_NO_SLOTS,
                "Offering has no weekly slots assigned: " + offeringId);
    }

    public static AppException lessonsAlreadyExist(UUID offeringId) {
        return Errors.of(HttpStatus.CONFLICT, CODE_LESSONS_ALREADY_EXIST,
                "Lessons already exist for offering: " + offeringId + ". Use regenerate to replace them.");
    }

    public static AppException semesterNotFound(UUID semesterId) {
        return Errors.of(HttpStatus.NOT_FOUND, CODE_SEMESTER_NOT_FOUND,
                "Semester not found: " + semesterId);
    }

    public static AppException curriculumSubjectNotFound(UUID curriculumSubjectId) {
        return Errors.of(HttpStatus.NOT_FOUND, CODE_CURRICULUM_SUBJECT_NOT_FOUND,
                "Curriculum subject not found: " + curriculumSubjectId);
    }

    public static AppException offeringNotFound(UUID offeringId) {
        return Errors.of(HttpStatus.NOT_FOUND, CODE_OFFERING_NOT_FOUND,
                "Offering not found: " + offeringId);
    }

    public static AppException timeslotNotResolved(UUID timeslotId) {
        return Errors.of(HttpStatus.NOT_FOUND, CODE_TIMESLOT_NOT_RESOLVED,
                "Timeslot could not be resolved: " + timeslotId);
    }
}
