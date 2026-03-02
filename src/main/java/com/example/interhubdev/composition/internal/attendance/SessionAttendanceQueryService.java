package com.example.interhubdev.composition.internal.attendance;

import com.example.interhubdev.absencenotice.AbsenceNoticeApi;
import com.example.interhubdev.absencenotice.AbsenceNoticeDto;
import com.example.interhubdev.absencenotice.StudentNoticeSummaryDto;
import com.example.interhubdev.attendancerecord.AttendanceRecordApi;
import com.example.interhubdev.attendancerecord.SessionRecordsDto;
import com.example.interhubdev.composition.SessionAttendanceQueryApi;
import com.example.interhubdev.composition.SessionAttendanceViewDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Use-case service: session attendance (records + notices merged) and session notices list.
 * Read-only aggregation from attendancerecord and absencenotice.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
class SessionAttendanceQueryService implements SessionAttendanceQueryApi {

    private final AttendanceRecordApi recordApi;
    private final AbsenceNoticeApi noticeApi;

    @Override
    public SessionAttendanceViewDto getSessionAttendance(UUID sessionId, UUID requesterId, boolean includeCanceled) {
        SessionRecordsDto records = recordApi.getSessionRecords(sessionId, requesterId);
        Map<UUID, List<StudentNoticeSummaryDto>> noticesByStudent =
                noticeApi.getSessionNotices(sessionId, includeCanceled);

        List<SessionAttendanceViewDto.SessionAttendanceStudentRowDto> students = new ArrayList<>();
        for (SessionRecordsDto.SessionRecordRowDto row : records.students()) {
            List<StudentNoticeSummaryDto> notices =
                    noticesByStudent.getOrDefault(row.studentId(), List.of());
            students.add(new SessionAttendanceViewDto.SessionAttendanceStudentRowDto(
                    row.studentId(),
                    row.status(),
                    row.minutesLate(),
                    row.teacherComment(),
                    row.markedAt(),
                    row.markedBy(),
                    row.absenceNoticeId(),
                    notices
            ));
        }
        return new SessionAttendanceViewDto(
                records.sessionId(),
                records.counts(),
                records.unmarkedCount(),
                students
        );
    }

    @Override
    public List<AbsenceNoticeDto> getSessionNotices(UUID sessionId, UUID requesterId, boolean includeCanceled) {
        recordApi.getSessionRecords(sessionId, requesterId);
        return noticeApi.getSessionNoticesAsList(sessionId, includeCanceled);
    }
}
