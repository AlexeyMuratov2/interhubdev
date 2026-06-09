package com.example.interhubdev.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class DotenvEnvironmentPostProcessorTest {

    @TempDir
    Path tempDir;

    @Test
    void loadsEnvFilesAndLetsLocalOverrideDefaultEnv() throws Exception {
        Files.writeString(tempDir.resolve(".env"), """
                MAIL_HOST=smtp.example.com
                MAIL_PASSWORD=from-default
                """, StandardCharsets.UTF_8);
        Files.writeString(tempDir.resolve(".env.local"), """
                MAIL_PASSWORD=from-local
                MAIL_FROM_NAME="InterHub Local"
                export MAIL_USE_SSL=false
                """, StandardCharsets.UTF_8);

        Map<String, Object> values = DotenvEnvironmentPostProcessor.loadFromRoots(
                java.util.List.of(tempDir));

        assertThat(values)
                .containsEntry("MAIL_HOST", "smtp.example.com")
                .containsEntry("MAIL_PASSWORD", "from-local")
                .containsEntry("MAIL_FROM_NAME", "InterHub Local")
                .containsEntry("MAIL_USE_SSL", "false");
    }
}
