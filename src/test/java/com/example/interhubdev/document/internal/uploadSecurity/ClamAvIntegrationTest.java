package com.example.interhubdev.document.internal.uploadSecurity;

import com.example.interhubdev.auth.AuthApi;
import com.example.interhubdev.document.StoragePort;
import com.example.interhubdev.user.Role;
import com.example.interhubdev.user.UserDto;
import com.example.interhubdev.user.UserStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Smoke integration test with real ClamAV (clamd) in Testcontainers.
 * Verifies that the app connects to clamd correctly, sends file content via INSTREAM,
 * and maps ClamAV detection to MALWARE_DETECTED.
 * <p>
 * Runs only when {@code RUN_CLAMAV_INTEGRATION=true} (e.g. in CI with Docker).
 */
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@Testcontainers
@EnabledIfEnvironmentVariable(named = "RUN_CLAMAV_INTEGRATION", matches = "true")
@DisplayName("ClamAV integration (real clamd)")
class ClamAvIntegrationTest {

    private static final UUID USER_ID = UUID.randomUUID();

    private static final String EICAR = "X5O!P%@AP[4\\PZX54(P^)7CC)7}$EICAR-STANDARD-ANTIVIRUS-TEST-FILE!$H+H*";

    @Container
    @SuppressWarnings("resource")
    private static final GenericContainer<?> CLAMAV = new GenericContainer<>(DockerImageName.parse("clamav/clamav:latest"))
            .withExposedPorts(3310)
            .waitingFor(Wait.forListeningPort().withStartupTimeout(Duration.ofMinutes(3)));

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthApi authApi;

    @MockitoBean
    private StoragePort storagePort;

    @DynamicPropertySource
    static void clamavProperties(DynamicPropertyRegistry registry) {
        registry.add("clamav.enabled", () -> "true");
        registry.add("clamav.host", CLAMAV::getHost);
        registry.add("clamav.port", () -> CLAMAV.getMappedPort(3310).toString());
    }

    private static UserDto authenticatedUser() {
        return new UserDto(
                USER_ID, "user@test.com", List.of(Role.STUDENT), UserStatus.ACTIVE,
                "Test", "User", null, null,
                LocalDateTime.now(), LocalDateTime.now(), LocalDateTime.now()
        );
    }

    @Test
    @DisplayName("EICAR in multipart returns 400 and MALWARE_DETECTED")
    void eicarUploadReturnsMalwareDetected() throws Exception {
        when(authApi.getCurrentUser(any())).thenReturn(Optional.of(authenticatedUser()));

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "eicar.txt",
                "text/plain",
                EICAR.getBytes()
        );

        mockMvc.perform(MockMvcRequestBuilders.multipart("/api/documents/upload").file(file))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(UploadSecurityErrors.CODE_MALWARE_DETECTED))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.message").value("File rejected"));
    }
}
