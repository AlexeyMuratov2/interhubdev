package com.example.interhubdev.group.internal;

import com.example.interhubdev.error.AppException;
import com.example.interhubdev.error.Errors;
import org.springframework.http.HttpStatus;

import java.util.UUID;

/**
 * Group module error codes and user-facing messages.
 * All errors are thrown as {@link AppException} and handled by global handler.
 */
public final class GroupErrors {

    private GroupErrors() {
    }

    /** Used when group is not found by id. */
    public static final String CODE_GROUP_NOT_FOUND = "GROUP_NOT_FOUND";
    /** Used when group leader record is not found. */
    public static final String CODE_GROUP_LEADER_NOT_FOUND = "GROUP_LEADER_NOT_FOUND";
    /** Used when curriculum override is not found. */
    public static final String CODE_GROUP_OVERRIDE_NOT_FOUND = "GROUP_OVERRIDE_NOT_FOUND";
    /** Used when group code already exists on create. */
    public static final String CODE_GROUP_CODE_EXISTS = "GROUP_CODE_EXISTS";
    /** Used when leader with same role already exists for group/student. */
    public static final String CODE_GROUP_LEADER_ROLE_EXISTS = "GROUP_LEADER_ROLE_EXISTS";

    public static AppException groupNotFound(UUID id) {
        return Errors.of(HttpStatus.NOT_FOUND, CODE_GROUP_NOT_FOUND, "Group not found: " + id);
    }

    public static AppException groupNotFoundByCode(String code) {
        return Errors.of(HttpStatus.NOT_FOUND, CODE_GROUP_NOT_FOUND, "Group not found with code: " + code);
    }

    public static AppException groupLeaderNotFound(UUID id) {
        return Errors.of(HttpStatus.NOT_FOUND, CODE_GROUP_LEADER_NOT_FOUND, "Group leader not found: " + id);
    }

    public static AppException overrideNotFound(UUID id) {
        return Errors.of(HttpStatus.NOT_FOUND, CODE_GROUP_OVERRIDE_NOT_FOUND, "Override not found: " + id);
    }

    public static AppException groupCodeExists(String code) {
        return Errors.of(HttpStatus.CONFLICT, CODE_GROUP_CODE_EXISTS, "Group with code '" + code + "' already exists");
    }

    public static AppException leaderRoleExists() {
        return Errors.of(HttpStatus.CONFLICT, CODE_GROUP_LEADER_ROLE_EXISTS,
                "Leader with this role already exists for group/student");
    }
}
