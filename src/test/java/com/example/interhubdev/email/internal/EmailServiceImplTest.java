package com.example.interhubdev.email.internal;

import com.example.interhubdev.email.EmailMessage;
import com.example.interhubdev.email.EmailResult;
import org.junit.jupiter.api.Test;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

class EmailServiceImplTest {

    @Test
    void returnsConfigurationFailureWhenSmtpPasswordIsMissing() {
        JavaMailSender mailSender = mock(JavaMailSender.class);
        EmailServiceImpl service = new EmailServiceImpl(
                mailSender,
                emailProperties(false),
                new MockEnvironment()
                        .withProperty("spring.mail.properties.mail.smtp.auth", "true")
                        .withProperty("spring.mail.username", "noreply@example.com")
                        .withProperty("spring.mail.password", ""));

        EmailResult result = service.send(EmailMessage.text(
                "user@example.com",
                "Test",
                "Hello"));

        assertThat(result.success()).isFalse();
        assertThat(result.error()).contains("MAIL_PASSWORD");
        verifyNoInteractions(mailSender);
    }

    @Test
    void logOnlyModeDoesNotRequireSmtpCredentials() {
        JavaMailSender mailSender = mock(JavaMailSender.class);
        EmailServiceImpl service = new EmailServiceImpl(
                mailSender,
                emailProperties(true),
                new MockEnvironment()
                        .withProperty("spring.mail.properties.mail.smtp.auth", "true"));

        EmailResult result = service.send(EmailMessage.text(
                "user@example.com",
                "Test",
                "Hello"));

        assertThat(result.success()).isTrue();
        verifyNoInteractions(mailSender);
    }

    private EmailProperties emailProperties(boolean logOnly) {
        EmailProperties properties = new EmailProperties();
        properties.setFrom("noreply@example.com");
        properties.setFromName("InterHubDev");
        properties.setEnabled(true);
        properties.setLogOnly(logOnly);
        return properties;
    }
}
