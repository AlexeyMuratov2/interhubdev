package com.example.interhubdev.schedule.internal;

import com.example.interhubdev.error.AppException;
import com.example.interhubdev.error.Errors;
import org.springframework.http.HttpStatus;

import java.util.UUID;

/**
 * Schedule module error factory. Stable error codes for API contract.
 * Do not instantiate.
 */
public final class ScheduleErrors {

    public static final String CODE_BUILDING_NOT_FOUND = "SCHEDULE_BUILDING_NOT_FOUND";
    public static final String CODE_ROOM_NOT_FOUND = "SCHEDULE_ROOM_NOT_FOUND";
    public static final String CODE_TIMESLOT_NOT_FOUND = "SCHEDULE_TIMESLOT_NOT_FOUND";
    public static final String CODE_LESSON_NOT_FOUND = "SCHEDULE_LESSON_NOT_FOUND";
    public static final String CODE_OFFERING_NOT_FOUND = "SCHEDULE_OFFERING_NOT_FOUND";
    /** Group not found (e.g. GET /lessons/group/{groupId} with non-existent group). */
    public static final String CODE_GROUP_NOT_FOUND = "SCHEDULE_GROUP_NOT_FOUND";
    public static final String CODE_BUILDING_HAS_ROOMS = "SCHEDULE_BUILDING_HAS_ROOMS";
    public static final String CODE_LESSON_ALREADY_EXISTS = "SCHEDULE_LESSON_ALREADY_EXISTS";

    private ScheduleErrors() {
    }

    public static AppException buildingNotFound(UUID id) {
        return Errors.of(HttpStatus.NOT_FOUND, CODE_BUILDING_NOT_FOUND, "Building not found: " + id);
    }

    public static AppException roomNotFound(UUID id) {
        return Errors.of(HttpStatus.NOT_FOUND, CODE_ROOM_NOT_FOUND, "Room not found: " + id);
    }

    public static AppException timeslotNotFound(UUID id) {
        return Errors.of(HttpStatus.NOT_FOUND, CODE_TIMESLOT_NOT_FOUND, "Timeslot not found: " + id);
    }

    public static AppException lessonNotFound(UUID id) {
        return Errors.of(HttpStatus.NOT_FOUND, CODE_LESSON_NOT_FOUND, "Lesson not found: " + id);
    }

    public static AppException offeringNotFound(UUID id) {
        return Errors.of(HttpStatus.NOT_FOUND, CODE_OFFERING_NOT_FOUND, "Offering not found: " + id);
    }

    public static AppException groupNotFound(UUID id) {
        return Errors.of(HttpStatus.NOT_FOUND, CODE_GROUP_NOT_FOUND, "Group not found: " + id);
    }

    public static AppException buildingHasRooms(UUID id) {
        return Errors.of(HttpStatus.CONFLICT, CODE_BUILDING_HAS_ROOMS, "Building has rooms; delete or reassign rooms first");
    }

    public static AppException lessonAlreadyExists() {
        return Errors.of(HttpStatus.CONFLICT, CODE_LESSON_ALREADY_EXISTS, "Lesson already exists for this offering, date and time");
    }
}
