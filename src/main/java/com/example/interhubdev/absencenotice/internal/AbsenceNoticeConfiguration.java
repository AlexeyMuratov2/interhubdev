package com.example.interhubdev.absencenotice.internal;

import com.example.interhubdev.offering.OfferingApi;
import com.example.interhubdev.schedule.ScheduleApi;
import com.example.interhubdev.student.StudentApi;
import com.example.interhubdev.teacher.TeacherApi;
import com.example.interhubdev.user.UserApi;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
class AbsenceNoticeConfiguration {

    private final ScheduleApi scheduleApi;
    private final StudentApi studentApi;
    private final UserApi userApi;
    private final TeacherApi teacherApi;
    private final OfferingApi offeringApi;

    @Bean("absenceNoticeSessionGateway")
    SessionGateway sessionGateway() {
        return new SessionGateway(scheduleApi);
    }

    @Bean("absenceNoticeRosterGateway")
    RosterGateway rosterGateway() {
        return new RosterGateway(studentApi);
    }

    @Bean
    AbsenceNoticeAccessPolicy absenceNoticeAccessPolicy() {
        return new AbsenceNoticeAccessPolicy(userApi, teacherApi, offeringApi);
    }
}
