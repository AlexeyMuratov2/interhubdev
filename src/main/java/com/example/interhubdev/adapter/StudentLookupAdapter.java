package com.example.interhubdev.adapter;

import com.example.interhubdev.schedule.StudentLookupPort;
import com.example.interhubdev.student.StudentApi;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

/**
 * Adapter: implements Schedule's StudentLookupPort using Student module's StudentApi.
 */
@Component
@RequiredArgsConstructor
public class StudentLookupAdapter implements StudentLookupPort {

    private final StudentApi studentApi;

    @Override
    public boolean hasStudentProfile(UUID userId) {
        return studentApi.existsByUserId(userId);
    }

    @Override
    public List<UUID> getGroupIdsByUserId(UUID userId) {
        return studentApi.getGroupIdsByUserId(userId);
    }
}
