package com.example.interhubdev.submission.internal.archive;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.zip.ZipInputStream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for ZipArchiveWriter: produces valid ZIP, entry names, skip on null stream.
 */
@DisplayName("ZipArchiveWriter")
class ZipArchiveWriterTest {

    @Test
    @DisplayName("writes valid ZIP with expected entry names")
    void writesValidZipWithEntryNames() throws IOException {
        ArchiveInfo info = new ArchiveInfo("Math", "HW1", LocalDate.of(2025, 2, 21));
        UUID studentId = UUID.randomUUID();
        ArchiveEntry e1 = new ArchiveEntry(studentId, "Ivanov", "HW1", LocalDate.of(2025, 2, 21),
            UUID.randomUUID(), "a.pdf", "pdf", 0);
        ArchiveEntry e2 = new ArchiveEntry(studentId, "Ivanov", "HW1", LocalDate.of(2025, 2, 21),
            UUID.randomUUID(), "b.pdf", "pdf", 1);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ZipArchiveWriter.write(out, info, List.of(e1, e2), entry -> {
            if (entry.equals(e1)) {
                return new InputStream() {
                    @Override
                    public int read() {
                        return -1;
                    }
                };
            }
            return new InputStream() {
                @Override
                public int read() {
                    return -1;
                }
            };
        });

        byte[] zipBytes = out.toByteArray();
        assertThat(zipBytes.length).isGreaterThan(0);
        assertThat(new String(zipBytes, 0, 2)).isEqualTo("PK");

        int entryCount = 0;
        try (ZipInputStream zis = new ZipInputStream(new java.io.ByteArrayInputStream(zipBytes))) {
            java.util.zip.ZipEntry ze;
            while ((ze = zis.getNextEntry()) != null) {
                entryCount++;
                assertThat(ze.getName()).contains(studentId.toString());
                assertThat(ze.getName()).contains("HW1");
                assertThat(ze.getName()).contains("Ivanov");
                zis.closeEntry();
            }
        }
        assertThat(entryCount).isEqualTo(2);
    }

    @Test
    @DisplayName("skips entry when openStream returns null")
    void skipsEntryWhenStreamNull() throws IOException {
        ArchiveInfo info = new ArchiveInfo("S", "H", LocalDate.of(2025, 1, 1));
        ArchiveEntry entry = new ArchiveEntry(UUID.randomUUID(), "N", "H", LocalDate.of(2025, 1, 1),
            UUID.randomUUID(), "f", "txt", 0);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ZipArchiveWriter.write(out, info, List.of(entry), e -> null);

        try (ZipInputStream zis = new ZipInputStream(new java.io.ByteArrayInputStream(out.toByteArray()))) {
            assertThat(zis.getNextEntry()).isNull();
        }
    }
}
