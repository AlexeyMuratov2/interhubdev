package com.example.interhubdev.offering;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Public API for Offering module: group subject offerings and offering teachers.
 */
public interface OfferingApi {

    Optional<GroupSubjectOfferingDto> findOfferingById(UUID id);

    List<GroupSubjectOfferingDto> findOfferingsByGroupId(UUID groupId);

    GroupSubjectOfferingDto createOffering(
            UUID groupId,
            UUID curriculumSubjectId,
            UUID teacherId,
            UUID roomId,
            String format,
            String notes
    );

    GroupSubjectOfferingDto updateOffering(
            UUID id,
            UUID teacherId,
            UUID roomId,
            String format,
            String notes
    );

    void deleteOffering(UUID id);

    List<OfferingTeacherDto> findTeachersByOfferingId(UUID offeringId);

    OfferingTeacherDto addOfferingTeacher(UUID offeringId, UUID teacherId, String role);

    void removeOfferingTeacher(UUID id);
}
