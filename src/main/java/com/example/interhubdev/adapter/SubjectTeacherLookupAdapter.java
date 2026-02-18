package com.example.interhubdev.adapter;

import com.example.interhubdev.schedule.TeacherLookupPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/**
 * Adapter: implements Subject's TeacherLookupPort using Schedule's TeacherLookupPort.
 * Reuses existing adapter implementation.
 */
@Component
@RequiredArgsConstructor
public class SubjectTeacherLookupAdapter implements com.example.interhubdev.subject.TeacherLookupPort {

    private final TeacherLookupPort scheduleTeacherLookupPort;

    @Override
    public Optional<UUID> getTeacherIdByUserId(UUID userId) {
        return scheduleTeacherLookupPort.getTeacherIdByUserId(userId);
    }
}
