package com.example.interhubdev.user;

/**
 * User account status.
 */
public enum UserStatus {
    /**
     * User has been invited but hasn't set their password yet.
     */
    PENDING,

    /**
     * User has activated their account and can log in.
     */
    ACTIVE,

    /**
     * User account has been disabled by an administrator.
     */
    DISABLED
}
