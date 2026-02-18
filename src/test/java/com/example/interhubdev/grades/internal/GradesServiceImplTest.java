package com.example.interhubdev.grades.internal;

import com.example.interhubdev.error.AppException;
import com.example.interhubdev.grades.*;
import com.example.interhubdev.group.GroupApi;
import com.example.interhubdev.offering.OfferingApi;
import com.example.interhubdev.offering.GroupSubjectOfferingDto;
import com.example.interhubdev.student.StudentApi;
import com.example.interhubdev.student.StudentDto;
import com.example.interhubdev.user.Role;
import com.example.interhubdev.user.UserApi;
import com.example.interhubdev.user.UserDto;
import com.example.interhubdev.user.UserStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for GradesServiceImpl: validation, void, breakdown sums.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("GradesServiceImpl")
class GradesServiceImplTest {

    private static final UUID TEACHER_ID = UUID.randomUUID();
    private static final UUID STUDENT_ID = UUID.randomUUID();
    private static final UUID OFFERING_ID = UUID.randomUUID();
    private static final UUID GROUP_ID = UUID.randomUUID();

    @Mock
    private GradeEntryRepository repository;
    @Mock
    private OfferingApi offeringApi;
    @Mock
    private StudentApi studentApi;
    @Mock
    private GroupApi groupApi;
    @Mock
    private UserApi userApi;

    @InjectMocks
    private GradesServiceImpl gradesService;

    private static UserDto teacher() {
        return new UserDto(
                TEACHER_ID, "t@test.com", List.of(Role.TEACHER), UserStatus.ACTIVE,
                "Teacher", "User", null, null,
                LocalDateTime.now(), LocalDateTime.now(), LocalDateTime.now()
        );
    }

    private static GroupSubjectOfferingDto offering(UUID groupId) {
        return new GroupSubjectOfferingDto(
                OFFERING_ID, groupId, UUID.randomUUID(), UUID.randomUUID(), null,
                "offline", null, LocalDateTime.now(), LocalDateTime.now()
        );
    }

    @Nested
    @DisplayName("create")
    class Create {

        @Test
        @DisplayName("CUSTOM without typeLabel throws validation")
        void customWithoutLabelThrows() {
            when(userApi.findById(TEACHER_ID)).thenReturn(Optional.of(teacher()));
            when(offeringApi.findOfferingById(OFFERING_ID)).thenReturn(Optional.of(offering(GROUP_ID)));
            when(studentApi.findById(STUDENT_ID)).thenReturn(Optional.of(new StudentDto(
                    STUDENT_ID, TEACHER_ID, "s1", "Name", "F", "C", 2023, "G1",
                    LocalDateTime.now(), LocalDateTime.now())));

            assertThatThrownBy(() -> gradesService.create(
                    STUDENT_ID, OFFERING_ID, BigDecimal.TEN,
                    GradeTypeCode.CUSTOM, null, null, null, null, null, TEACHER_ID))
                    .isInstanceOf(AppException.class)
                    .hasMessageContaining("typeLabel is required when typeCode is CUSTOM");
            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("forbidden when user cannot grade")
        void forbiddenWhenNotTeacher() {
            UserDto student = new UserDto(
                    TEACHER_ID, "s@test.com", List.of(Role.STUDENT), UserStatus.ACTIVE,
                    "S", "U", null, null,
                    LocalDateTime.now(), LocalDateTime.now(), LocalDateTime.now());
            when(userApi.findById(TEACHER_ID)).thenReturn(Optional.of(student));

            assertThatThrownBy(() -> gradesService.create(
                    STUDENT_ID, OFFERING_ID, BigDecimal.TEN,
                    GradeTypeCode.SEMINAR, null, null, null, null, null, TEACHER_ID))
                    .isInstanceOf(AppException.class)
                    .hasMessageContaining("Only teachers or administrators");
            verify(repository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("voidEntry")
    class VoidEntry {

        @Test
        @DisplayName("sets status to VOIDED")
        void setsStatusVoided() {
            when(userApi.findById(TEACHER_ID)).thenReturn(Optional.of(teacher()));
            UUID id = UUID.randomUUID();
            GradeEntryEntity entity = GradeEntryEntity.builder()
                    .id(id)
                    .studentId(STUDENT_ID)
                    .offeringId(OFFERING_ID)
                    .points(BigDecimal.TEN)
                    .typeCode(GradeTypeCode.SEMINAR)
                    .gradedBy(TEACHER_ID)
                    .gradedAt(LocalDateTime.now())
                    .status(GradeEntryEntity.STATUS_ACTIVE)
                    .build();
            when(repository.findById(id)).thenReturn(Optional.of(entity));
            when(repository.save(any())).thenAnswer(i -> i.getArgument(0));

            gradesService.voidEntry(id, TEACHER_ID);

            assertThat(entity.getStatus()).isEqualTo(GradeEntryEntity.STATUS_VOIDED);
            verify(repository).save(entity);
        }

        @Test
        @DisplayName("throws when entry not found")
        void notFound() {
            when(userApi.findById(TEACHER_ID)).thenReturn(Optional.of(teacher()));
            UUID id = UUID.randomUUID();
            when(repository.findById(id)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> gradesService.voidEntry(id, TEACHER_ID))
                    .isInstanceOf(AppException.class)
                    .hasMessageContaining("Grade entry not found");
        }
    }

    @Nested
    @DisplayName("getStudentOfferingGrades")
    class GetStudentOfferingGrades {

        @Test
        @DisplayName("breakdown sums only ACTIVE entries by type")
        void breakdownSumsActiveOnly() {
            when(userApi.findById(TEACHER_ID)).thenReturn(Optional.of(teacher()));
            when(offeringApi.findOfferingById(OFFERING_ID)).thenReturn(Optional.of(offering(GROUP_ID)));
            UUID id1 = UUID.randomUUID();
            UUID id2 = UUID.randomUUID();
            List<GradeEntryEntity> entries = List.of(
                    entity(id1, BigDecimal.TEN, GradeTypeCode.SEMINAR, GradeEntryEntity.STATUS_ACTIVE),
                    entity(id2, new BigDecimal("5"), GradeTypeCode.SEMINAR, GradeEntryEntity.STATUS_ACTIVE),
                    entity(UUID.randomUUID(), new BigDecimal("100"), GradeTypeCode.EXAM, GradeEntryEntity.STATUS_VOIDED)
            );
            when(repository.findByStudentIdAndOfferingIdAndGradedAtBetween(
                    eq(STUDENT_ID), eq(OFFERING_ID), eq(null), eq(null)))
                    .thenReturn(entries);

            StudentOfferingGradesDto result = gradesService.getStudentOfferingGrades(
                    STUDENT_ID, OFFERING_ID, null, null, false, TEACHER_ID);

            assertThat(result.totalPoints()).isEqualByComparingTo(new BigDecimal("15")); // 10 + 5, 100 is VOIDED
            assertThat(result.breakdownByType()).containsEntry("SEMINAR", new BigDecimal("15"));
            assertThat(result.breakdownByType()).doesNotContainKey("EXAM");
        }

        private GradeEntryEntity entity(UUID id, BigDecimal points, GradeTypeCode type, String status) {
            GradeEntryEntity e = new GradeEntryEntity();
            e.setId(id);
            e.setStudentId(STUDENT_ID);
            e.setOfferingId(OFFERING_ID);
            e.setPoints(points);
            e.setTypeCode(type);
            e.setGradedBy(TEACHER_ID);
            e.setGradedAt(LocalDateTime.now());
            e.setStatus(status);
            return e;
        }
    }
}
