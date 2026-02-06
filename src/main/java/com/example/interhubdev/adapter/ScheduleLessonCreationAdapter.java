package com.example.interhubdev.adapter;

import com.example.interhubdev.offering.LessonCreationPort;
import com.example.interhubdev.schedule.LessonBulkCreateRequest;
import com.example.interhubdev.schedule.ScheduleApi;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Adapter: implements Offering module's LessonCreationPort using Schedule module's ScheduleApi.
 * Provides bulk lesson creation and deletion for lesson generation.
 */
@Component
@RequiredArgsConstructor
public class ScheduleLessonCreationAdapter implements LessonCreationPort {

    private final ScheduleApi scheduleApi;

    @Override
    public int createLessonsInBulk(List<LessonCreateCommand> commands) {
        List<LessonBulkCreateRequest> requests = commands.stream()
                .map(cmd -> new LessonBulkCreateRequest(
                        cmd.offeringId(),
                        cmd.date(),
                        cmd.startTime(),
                        cmd.endTime(),
                        cmd.timeslotId(),
                        cmd.roomId(),
                        cmd.status()
                ))
                .toList();
        return scheduleApi.createLessonsInBulk(requests).size();
    }

    @Override
    public void deleteLessonsByOfferingId(java.util.UUID offeringId) {
        scheduleApi.deleteLessonsByOfferingId(offeringId);
    }
}
