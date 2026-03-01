package com.example.interhubdev.attendancerecord.internal;

import com.example.interhubdev.offering.OfferingApi;
import com.example.interhubdev.schedule.ScheduleApi;
import com.example.interhubdev.student.StudentApi;
import com.example.interhubdev.teacher.TeacherApi;
import com.example.interhubdev.user.UserApi;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for shared components (gateways, access policy).
 */
@Configuration
@RequiredArgsConstructor
class AttendanceRecordSharedConfiguration {

    private final ScheduleApi scheduleApi;
    private final StudentApi studentApi;
    private final UserApi userApi;
    private final TeacherApi teacherApi;
    private final OfferingApi offeringApi;

    @Bean("attendanceRecordSessionGateway")
    SessionGateway sessionGateway() {
        return new SessionGateway(scheduleApi);
    }

    @Bean("attendanceRecordRosterGateway")
    RosterGateway rosterGateway() {
        return new RosterGateway(studentApi);
    }

    @Bean
    AttendanceRecordAccessPolicy attendanceRecordAccessPolicy() {
        return new AttendanceRecordAccessPolicy(userApi, teacherApi, offeringApi);
    }
}
