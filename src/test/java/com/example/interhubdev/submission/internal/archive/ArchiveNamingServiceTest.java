package com.example.interhubdev.submission.internal.archive;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for ArchiveNamingService: sanitize, archive filename, entry filename, truncation.
 */
@DisplayName("ArchiveNamingService")
class ArchiveNamingServiceTest {

    @Nested
    @DisplayName("sanitize")
    class Sanitize {

        @Test
        @DisplayName("replaces forbidden chars with space and collapses spaces")
        void forbiddenChars() {
            assertThat(ArchiveNamingService.sanitize("a/b\\c*d?e")).isEqualTo("a b c d e");
            assertThat(ArchiveNamingService.sanitize("a  b")).isEqualTo("a b");
        }

        @Test
        @DisplayName("null becomes empty then unnamed")
        void nullBecomesUnnamed() {
            assertThat(ArchiveNamingService.sanitize(null)).isEqualTo("");
        }

        @Test
        @DisplayName("blank after trim becomes unnamed")
        void blankBecomesUnnamed() {
            assertThat(ArchiveNamingService.sanitize("   ")).isEqualTo("unnamed");
        }

        @Test
        @DisplayName("preserves safe characters")
        void preservesSafe() {
            assertThat(ArchiveNamingService.sanitize("Math 101")).isEqualTo("Math 101");
            assertThat(ArchiveNamingService.sanitize("Homework-1")).isEqualTo("Homework-1");
        }
    }

    @Nested
    @DisplayName("buildArchiveFilename")
    class BuildArchiveFilename {

        @Test
        @DisplayName("formats as subjectName - homeworkTitle - lessonDate.zip")
        void format() {
            ArchiveInfo info = new ArchiveInfo("Math", "HW1", LocalDate.of(2025, 2, 21));
            assertThat(ArchiveNamingService.buildArchiveFilename(info))
                .isEqualTo("Math - HW1 - 2025-02-21.zip");
        }

        @Test
        @DisplayName("sanitizes components")
        void sanitizes() {
            ArchiveInfo info = new ArchiveInfo("Math/101", "HW: 1", LocalDate.of(2025, 2, 21));
            assertThat(ArchiveNamingService.buildArchiveFilename(info))
                .contains("2025-02-21.zip")
                .doesNotContain("/")
                .doesNotContain(":");
        }

        @Test
        @DisplayName("truncates long names")
        void truncates() {
            String longName = "a".repeat(300);
            ArchiveInfo info = new ArchiveInfo(longName, "HW", LocalDate.of(2025, 2, 21));
            String name = ArchiveNamingService.buildArchiveFilename(info);
            assertThat(name).endsWith(".zip");
            assertThat(name.length()).isLessThanOrEqualTo(200);
        }
    }

    @Nested
    @DisplayName("buildEntryFilename")
    class BuildEntryFilename {

        @Test
        @DisplayName("formats as studentId - homeworkTitle - studentName - lessonDate.ext")
        void format() {
            ArchiveEntry entry = new ArchiveEntry(
                java.util.UUID.randomUUID(),
                "Ivanov Ivan",
                "HW1",
                LocalDate.of(2025, 2, 21),
                java.util.UUID.randomUUID(),
                "file.pdf",
                "pdf",
                0
            );
            String name = ArchiveNamingService.buildEntryFilename(entry);
            assertThat(name).contains(entry.studentId().toString());
            assertThat(name).contains("Ivanov Ivan");
            assertThat(name).contains("HW1");
            assertThat(name).contains("2025-02-21");
            assertThat(name).endsWith(".pdf");
        }

        @Test
        @DisplayName("adds _N suffix for fileIndex > 0 for uniqueness")
        void uniquenessSuffix() {
            ArchiveEntry entry = new ArchiveEntry(
                java.util.UUID.randomUUID(),
                "Student",
                "HW",
                LocalDate.of(2025, 2, 21),
                java.util.UUID.randomUUID(),
                "a.pdf",
                "pdf",
                2
            );
            String name = ArchiveNamingService.buildEntryFilename(entry);
            assertThat(name).contains("_2");
            assertThat(name).endsWith(".pdf");
        }

        @Test
        @DisplayName("handles missing extension")
        void noExtension() {
            ArchiveEntry entry = new ArchiveEntry(
                java.util.UUID.randomUUID(),
                "S",
                "HW",
                LocalDate.of(2025, 2, 21),
                java.util.UUID.randomUUID(),
                "file",
                "",
                0
            );
            String name = ArchiveNamingService.buildEntryFilename(entry);
            assertThat(name).doesNotContain("..");
            assertThat(name).endsWith("2025-02-21");
        }
    }
}
