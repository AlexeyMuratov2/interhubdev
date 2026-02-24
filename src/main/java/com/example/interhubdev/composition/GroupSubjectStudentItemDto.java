package com.example.interhubdev.composition;

import com.example.interhubdev.student.StudentDto;
import com.example.interhubdev.user.UserDto;

import java.math.BigDecimal;

/**
 * One student row in the group subject info screen: profile, points, homework submissions, attendance.
 * LATE is not counted as absence; EXCUSED is counted as absence for attendance percent.
 */
public record GroupSubjectStudentItemDto(
    StudentDto student,
    UserDto user,
    /** Current total points for this subject (offering). */
    BigDecimal totalPoints,
    /** Number of homework assignments submitted this semester. */
    int submittedHomeworkCount,
    /** Attendance percentage from attendance module: (PRESENT + LATE) / lessonsWithAtLeastOneMark * 100. Null if no such lessons. */
    Double attendancePercent
) {
}
