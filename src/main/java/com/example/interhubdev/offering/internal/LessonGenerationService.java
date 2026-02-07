package com.example.interhubdev.offering.internal;

import com.example.interhubdev.academic.AcademicApi;
import com.example.interhubdev.academic.SemesterDto;
import com.example.interhubdev.offering.LessonCreationPort;
import com.example.interhubdev.offering.LessonCreationPort.LessonCreateCommand;
import com.example.interhubdev.program.CurriculumSubjectDto;
import com.example.interhubdev.program.ProgramApi;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Core service for automatic lesson generation.
 * Uses offering slots' own day and time (slot owns time; timeslot was UI hint only).
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
class LessonGenerationService {

    private final GroupSubjectOfferingRepository offeringRepository;
    private final OfferingSlotRepository slotRepository;
    private final ProgramApi programApi;
    private final AcademicApi academicApi;
    private final LessonCreationPort lessonCreationPort;

    @Transactional
    int generateForOffering(UUID offeringId, UUID semesterId) {
        GroupSubjectOffering offering = offeringRepository.findById(offeringId)
                .orElseThrow(() -> OfferingErrors.offeringNotFound(offeringId));

        SemesterDto semester = academicApi.findSemesterById(semesterId)
                .orElseThrow(() -> OfferingErrors.semesterNotFound(semesterId));

        CurriculumSubjectDto currSubject = programApi.findCurriculumSubjectById(offering.getCurriculumSubjectId())
                .orElseThrow(() -> OfferingErrors.curriculumSubjectNotFound(offering.getCurriculumSubjectId()));

        List<OfferingSlot> slots = slotRepository.findByOfferingIdOrderByDayOfWeekAscStartTimeAsc(offeringId);
        if (slots.isEmpty()) {
            throw OfferingErrors.noSlots(offeringId);
        }

        List<LessonCreateCommand> commands = buildLessonCommands(offering, slots, semester, currSubject);
        if (commands.isEmpty()) {
            log.info("No lessons to generate for offering {} in semester {}", offeringId, semesterId);
            return 0;
        }

        int created = lessonCreationPort.createLessonsInBulk(commands);
        log.info("Generated {} lessons for offering {} in semester {}", created, offeringId, semesterId);
        return created;
    }

    @Transactional
    int generateForGroup(UUID groupId, UUID semesterId) {
        SemesterDto semester = academicApi.findSemesterById(semesterId)
                .orElseThrow(() -> OfferingErrors.semesterNotFound(semesterId));

        List<GroupSubjectOffering> offerings = offeringRepository
                .findByGroupIdOrderByCurriculumSubjectIdAsc(groupId);

        int totalCreated = 0;
        for (GroupSubjectOffering offering : offerings) {
            List<OfferingSlot> slots = slotRepository
                    .findByOfferingIdOrderByDayOfWeekAscStartTimeAsc(offering.getId());
            if (slots.isEmpty()) {
                log.debug("Skipping offering {} (no slots)", offering.getId());
                continue;
            }

            CurriculumSubjectDto currSubject = programApi
                    .findCurriculumSubjectById(offering.getCurriculumSubjectId())
                    .orElse(null);
            if (currSubject == null) {
                log.warn("Curriculum subject {} not found for offering {}, skipping",
                        offering.getCurriculumSubjectId(), offering.getId());
                continue;
            }

            List<LessonCreateCommand> commands = buildLessonCommands(offering, slots, semester, currSubject);
            if (!commands.isEmpty()) {
                int created = lessonCreationPort.createLessonsInBulk(commands);
                totalCreated += created;
                log.info("Generated {} lessons for offering {} in semester {}",
                        created, offering.getId(), semesterId);
            }
        }

        log.info("Total: generated {} lessons for group {} in semester {}", totalCreated, groupId, semesterId);
        return totalCreated;
    }

    @Transactional
    int regenerateForOffering(UUID offeringId, UUID semesterId) {
        if (!offeringRepository.existsById(offeringId)) {
            throw OfferingErrors.offeringNotFound(offeringId);
        }
        SemesterDto semester = academicApi.findSemesterById(semesterId)
                .orElseThrow(() -> OfferingErrors.semesterNotFound(semesterId));
        lessonCreationPort.deleteLessonsByOfferingIdAndDateRange(
                offeringId, semester.startDate(), semester.endDate());
        log.info("Deleted lessons for offering {} in semester date range [{}..{}] before regeneration",
                offeringId, semester.startDate(), semester.endDate());
        return generateForOffering(offeringId, semesterId);
    }

    private List<LessonCreateCommand> buildLessonCommands(
            GroupSubjectOffering offering,
            List<OfferingSlot> slots,
            SemesterDto semester,
            CurriculumSubjectDto currSubject
    ) {
        List<LessonCreateCommand> commands = new ArrayList<>();
        LocalDate semesterStart = semester.startDate();
        LocalDate semesterEnd = semester.endDate();
        int durationWeeks = currSubject.durationWeeks();

        for (OfferingSlot slot : slots) {
            DayOfWeek targetDay = DayOfWeek.of(slot.getDayOfWeek());
            LocalDate firstDate = semesterStart.getDayOfWeek() == targetDay
                    ? semesterStart
                    : semesterStart.with(TemporalAdjusters.next(targetDay));

            UUID effectiveRoom = slot.getRoomId() != null ? slot.getRoomId() : offering.getRoomId();

            for (int week = 0; week < durationWeeks; week++) {
                LocalDate lessonDate = firstDate.plusWeeks(week);
                if (lessonDate.isAfter(semesterEnd)) {
                    break;
                }
                commands.add(new LessonCreateCommand(
                        offering.getId(),
                        slot.getId(),
                        lessonDate,
                        slot.getStartTime(),
                        slot.getEndTime(),
                        slot.getTimeslotId(),
                        effectiveRoom,
                        "planned"
                ));
            }
        }
        return commands;
    }
}
