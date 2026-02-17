package com.example.interhubdev.adapter;

import com.example.interhubdev.document.LessonLookupPort;
import com.example.interhubdev.schedule.ScheduleApi;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Adapter: implements Document module's LessonLookupPort using Schedule module's ScheduleApi.
 * Allows homework (and other document features) to validate lesson id without document depending on schedule.
 */
@Component
@RequiredArgsConstructor
public class LessonLookupAdapter implements LessonLookupPort {

    private final ScheduleApi scheduleApi;

    @Override
    public boolean existsById(UUID lessonId) {
        return scheduleApi.findLessonById(lessonId).isPresent();
    }
}
