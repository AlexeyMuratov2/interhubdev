package com.example.interhubdev.adapter;

import com.example.interhubdev.schedule.TeacherLookupPort;
import com.example.interhubdev.schedule.TeacherSummaryDto;
import com.example.interhubdev.teacher.TeacherApi;
import com.example.interhubdev.teacher.TeacherDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Adapter: implements Schedule's TeacherLookupPort using Teacher module's TeacherApi.
 */
@Component
@RequiredArgsConstructor
public class TeacherLookupAdapter implements TeacherLookupPort {

    private final TeacherApi teacherApi;

    @Override
    public Map<UUID, TeacherSummaryDto> getTeacherSummaries(List<UUID> teacherIds) {
        if (teacherIds == null || teacherIds.isEmpty()) {
            return Map.of();
        }
        List<TeacherDto> teachers = teacherApi.findByIds(teacherIds);
        return teachers.stream()
                .collect(Collectors.toMap(TeacherDto::id, dto -> new TeacherSummaryDto(dto.id(), displayName(dto))));
    }

    @Override
    public Optional<UUID> getTeacherIdByUserId(UUID userId) {
        return teacherApi.findByUserId(userId).map(TeacherDto::id);
    }

    private static String displayName(TeacherDto dto) {
        if (dto.englishName() != null && !dto.englishName().isBlank()) {
            return dto.englishName();
        }
        if (dto.teacherId() != null && !dto.teacherId().isBlank()) {
            return dto.teacherId();
        }
        return dto.id() != null ? dto.id().toString() : "";
    }
}
