package com.example.interhubdev.submission.internal.archive;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.function.Function;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Writes a ZIP archive to an output stream. Streams entry content one-by-one to avoid loading
 * the entire archive into memory. Caller provides a function to open an InputStream for each
 * stored file; the writer closes each stream after copying.
 */
@Slf4j
public final class ZipArchiveWriter {

    private static final int COPY_BUFFER_SIZE = 8192;

    private ZipArchiveWriter() {
    }

    /**
     * Write ZIP to {@code out} with the given archive-level filename, and one entry per
     * {@link ArchiveEntry}. For each entry, {@code openStream} is called with the entry's
     * storedFileId; the returned InputStream is read and then closed. If openStream throws or
     * returns null, the entry is skipped and the error is logged (strategy: skip problematic
     * files and continue).
     *
     * @param out          target stream (caller closes)
     * @param archiveInfo  used to build archive filename (for reference; actual filename is set by controller)
     * @param entries      list of entries to add
     * @param openStream   function to open stream for each stored file (will be closed by writer)
     */
    public static void write(
        OutputStream out,
        ArchiveInfo archiveInfo,
        List<ArchiveEntry> entries,
        Function<ArchiveEntry, InputStream> openStream
    ) throws IOException {
        try (ZipOutputStream zos = new ZipOutputStream(out)) {
            for (ArchiveEntry entry : entries) {
                String entryName = ArchiveNamingService.buildEntryFilename(entry);
                InputStream is;
                try {
                    is = openStream.apply(entry);
                } catch (Exception e) {
                    log.warn("Skip archive entry {}: failed to open storedFileId {} - {}", entryName, entry.storedFileId(), e.getMessage());
                    continue;
                }
                if (is == null) {
                    log.warn("Skip archive entry {}: no stream for storedFileId {}", entryName, entry.storedFileId());
                    continue;
                }
                ZipEntry ze = new ZipEntry(entryName);
                ze.setTime(System.currentTimeMillis());
                zos.putNextEntry(ze);
                try {
                    copy(is, zos);
                } catch (Exception e) {
                    log.warn("Skip archive entry {}: failed to read storedFileId {} - {}", entryName, entry.storedFileId(), e.getMessage());
                } finally {
                    is.close();
                }
                zos.closeEntry();
            }
        }
    }

    private static void copy(InputStream in, OutputStream out) throws IOException {
        byte[] buf = new byte[COPY_BUFFER_SIZE];
        int n;
        while ((n = in.read(buf)) != -1) {
            out.write(buf, 0, n);
        }
    }
}
