package com.example.interhubdev.attendance.internal;

import com.example.interhubdev.attendance.AttendanceRecordDto;
import com.example.interhubdev.offering.GroupSubjectOfferingDto;
import com.example.interhubdev.offering.OfferingApi;
import com.example.interhubdev.schedule.LessonDto;
import com.example.interhubdev.schedule.ScheduleApi;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Use case for teachers to detach an absence notice from an attendance record.
 * Teacher must have permission to manage the session of the record.
 */
@Service
@RequiredArgsConstructor
@Transactional
class DetachAbsenceNoticeFromAttendanceUseCase {

    private final AttendanceRecordRepository recordRepository;
    private final ScheduleApi scheduleApi;
    private final OfferingApi offeringApi;
    private final AttendanceAccessPolicy accessPolicy;

    /**
     * Detach absence notice from an attendance record.
     *
     * @param recordId  attendance record ID
     * @param requesterId user ID of requester (must be teacher of session or admin)
     * @return updated attendance record DTO
     * @throws com.example.interhubdev.error.AppException if record not found or access denied
     */
    AttendanceRecordDto execute(UUID recordId, UUID requesterId) {
        // Load record
        AttendanceRecord record = recordRepository.findById(recordId)
                .orElseThrow(() -> AttendanceErrors.recordNotFound(recordId));

        // Check authorization: teacher must be able to manage the session
        LessonDto lesson = scheduleApi.findLessonById(record.getLessonSessionId())
                .orElseThrow(() -> AttendanceErrors.lessonNotFound(record.getLessonSessionId()));
        GroupSubjectOfferingDto offering = offeringApi.findOfferingById(lesson.offeringId())
                .orElseThrow(() -> AttendanceErrors.offeringNotFound(lesson.offeringId()));
        accessPolicy.ensureCanManageSession(requesterId, lesson);

        // Detach notice
        record.setAbsenceNoticeId(null);
        AttendanceRecord saved = recordRepository.save(record);

        return AttendanceMappers.toDto(saved);
    }
}
