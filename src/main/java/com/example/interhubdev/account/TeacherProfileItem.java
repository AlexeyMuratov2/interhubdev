package com.example.interhubdev.account;

import com.example.interhubdev.teacher.TeacherDto;

/**
 * Teacher profile with display name for list and get-one responses.
 * Display name: teacher's englishName if set, otherwise user's full name.
 */
public record TeacherProfileItem(
        TeacherDto profile,
        String displayName
) {
}
