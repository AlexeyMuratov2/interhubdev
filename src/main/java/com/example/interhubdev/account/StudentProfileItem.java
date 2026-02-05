package com.example.interhubdev.account;

import com.example.interhubdev.student.StudentDto;

/**
 * Student profile with display name for list and get-one responses.
 * Display name: student's chineseName if set, otherwise user's full name.
 */
public record StudentProfileItem(
        StudentDto profile,
        String displayName
) {
}
