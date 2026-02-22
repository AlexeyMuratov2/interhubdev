package com.example.interhubdev.submission.internal;

import com.example.interhubdev.auth.AuthApi;
import com.example.interhubdev.error.AppException;
import com.example.interhubdev.submission.SubmissionApi;
import com.example.interhubdev.submission.SubmissionsArchiveHandle;
import com.example.interhubdev.user.Role;
import com.example.interhubdev.user.UserDto;
import com.example.interhubdev.user.UserStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.io.IOException;
import java.io.OutputStream;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests for submissions archive download endpoint: headers, zip content, 403, 404.
 */
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@DisplayName("SubmissionController archive download")
class SubmissionControllerArchiveTest {

    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID HOMEWORK_ID = UUID.randomUUID();

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthApi authApi;

    @MockitoBean
    private SubmissionApi submissionApi;

    private static UserDto teacher() {
        return new UserDto(
            USER_ID, "t@test.com", List.of(Role.TEACHER), UserStatus.ACTIVE,
            "Teacher", "User", null, null,
            LocalDateTime.now(), LocalDateTime.now(), LocalDateTime.now()
        );
    }

    private static SubmissionsArchiveHandle handleWithMinimalZip(String filename) {
        return new SubmissionsArchiveHandle() {
            @Override
            public String getFilename() {
                return filename;
            }

            @Override
            public void writeTo(OutputStream out) throws IOException {
                try (ZipOutputStream zos = new ZipOutputStream(out)) {
                    zos.putNextEntry(new ZipEntry("test.txt"));
                    zos.write("hello".getBytes());
                    zos.closeEntry();
                }
            }
        };
    }

    @Nested
    @DisplayName("GET /api/homework/{homeworkId}/submissions/archive")
    class DownloadArchive {

        @Test
        @DisplayName("returns 200 with application/zip and Content-Disposition attachment")
        void success_headersAndZip() throws Exception {
            when(authApi.getCurrentUser(any())).thenReturn(Optional.of(teacher()));
            String filename = "Math - HW1 - 2025-02-21.zip";
            when(submissionApi.buildSubmissionsArchive(eq(HOMEWORK_ID), eq(USER_ID)))
                .thenReturn(handleWithMinimalZip(filename));

            byte[] body = mockMvc.perform(get("/api/homework/{homeworkId}/submissions/archive", HOMEWORK_ID))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "application/zip"))
                .andExpect(header().exists("Content-Disposition"))
                .andReturn()
                .getResponse()
                .getContentAsByteArray();

            assertThat(body).isNotEmpty();
            assertThat(new String(body, 0, 2)).isEqualTo("PK");
            verify(submissionApi).buildSubmissionsArchive(eq(HOMEWORK_ID), eq(USER_ID));
        }

        @Test
        @DisplayName("Content-Disposition contains attachment and filename")
        void contentDispositionFilename() throws Exception {
            when(authApi.getCurrentUser(any())).thenReturn(Optional.of(teacher()));
            String filename = "Subject - Homework - 2025-02-21.zip";
            when(submissionApi.buildSubmissionsArchive(eq(HOMEWORK_ID), eq(USER_ID)))
                .thenReturn(handleWithMinimalZip(filename));

            String disposition = mockMvc.perform(get("/api/homework/{homeworkId}/submissions/archive", HOMEWORK_ID))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getHeader("Content-Disposition");
            assertThat(disposition).isNotNull();
            assertThat(disposition).contains("attachment");
            assertThat(disposition).contains("filename");
        }

        @Test
        @DisplayName("returns 403 when user has no permission")
        void forbidden() throws Exception {
            when(authApi.getCurrentUser(any())).thenReturn(Optional.of(teacher()));
            when(submissionApi.buildSubmissionsArchive(eq(HOMEWORK_ID), eq(USER_ID)))
                .thenThrow(new AppException("FORBIDDEN", HttpStatus.FORBIDDEN, "Forbidden", null));

            mockMvc.perform(get("/api/homework/{homeworkId}/submissions/archive", HOMEWORK_ID))
                .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("returns 404 when homework not found")
        void notFound() throws Exception {
            when(authApi.getCurrentUser(any())).thenReturn(Optional.of(teacher()));
            when(submissionApi.buildSubmissionsArchive(eq(HOMEWORK_ID), eq(USER_ID)))
                .thenThrow(new AppException("NOT_FOUND", HttpStatus.NOT_FOUND, "Not found", null));

            mockMvc.perform(get("/api/homework/{homeworkId}/submissions/archive", HOMEWORK_ID))
                .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("returns 401 when not authenticated")
        void unauthorized() throws Exception {
            when(authApi.getCurrentUser(any())).thenReturn(Optional.empty());

            mockMvc.perform(get("/api/homework/{homeworkId}/submissions/archive", HOMEWORK_ID))
                .andExpect(status().isUnauthorized());
            verify(submissionApi, never()).buildSubmissionsArchive(any(), any());
        }
    }
}
