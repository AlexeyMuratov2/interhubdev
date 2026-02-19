package com.example.interhubdev.attendance.internal;

import com.example.interhubdev.offering.OfferingApi;
import com.example.interhubdev.schedule.ScheduleApi;
import com.example.interhubdev.student.StudentApi;
import com.example.interhubdev.teacher.TeacherApi;
import com.example.interhubdev.user.UserApi;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for shared components used by attendance subsystems.
 */
@Configuration
@RequiredArgsConstructor
class AttendanceSharedConfiguration {

    private final ScheduleApi scheduleApi;
    private final StudentApi studentApi;
    private final UserApi userApi;
    private final TeacherApi teacherApi;
    private final OfferingApi offeringApi;

    @Bean
    SessionGateway sessionGateway() {
        return new SessionGateway(scheduleApi);
    }

    @Bean
    RosterGateway rosterGateway() {
        return new RosterGateway(studentApi);
    }

    @Bean
    AttendanceAccessPolicy attendanceAccessPolicy() {
        return new AttendanceAccessPolicy(userApi, teacherApi, offeringApi);
    }
}
