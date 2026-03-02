package com.example.interhubdev.account;

import com.example.interhubdev.student.StudentDto;

/**
 * Student profile with display name for list and get-one responses.
 * Display name: student's chineseName if set, otherwise user's full name (never empty; see {@link com.example.interhubdev.student.StudentApi#studentDisplayName}).
 */
public record StudentProfileItem(
        StudentDto profile,
        String displayName
) {
}
