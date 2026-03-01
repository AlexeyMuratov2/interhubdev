package com.example.interhubdev.otp.internal;

import com.example.interhubdev.error.AppException;
import com.example.interhubdev.otp.OtpApi;
import com.example.interhubdev.otp.OtpCreatedResult;
import com.example.interhubdev.otp.OtpOptions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Unit tests for OtpServiceImpl.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("OtpServiceImpl")
class OtpServiceImplTest {

    private static final String PURPOSE = "password-change";
    private static final String SUBJECT = "user-123";

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOps;

    @Mock
    private OtpProperties properties;

    @InjectMocks
    private OtpServiceImpl otpService;

    @Nested
    @DisplayName("create")
    class Create {

        @Test
        @DisplayName("returns plain code and expiresAt when rate limit not set")
        void success() {
            when(redisTemplate.opsForValue()).thenReturn(valueOps);
            when(redisTemplate.hasKey(anyString())).thenReturn(false);
            when(properties.getDefaultTtlMinutes()).thenReturn(15);
            when(properties.getDefaultCodeLength()).thenReturn(6);
            when(properties.getMinRateLimitSeconds()).thenReturn(60);

            OtpCreatedResult result = otpService.create(PURPOSE, SUBJECT, OtpOptions.defaults());

            assertThat(result.plainCode()).hasSize(6).matches("\\d+");
            assertThat(result.expiresAt()).isAfter(Instant.now());
        }

        @Test
        @DisplayName("throws when purpose is blank")
        void blankPurpose() {
            assertThatThrownBy(() -> otpService.create("", SUBJECT, OtpOptions.defaults()))
                    .isInstanceOf(AppException.class);
        }

        @Test
        @DisplayName("throws when subject is blank")
        void blankSubject() {
            assertThatThrownBy(() -> otpService.create(PURPOSE, "", OtpOptions.defaults()))
                    .isInstanceOf(AppException.class);
        }

        @Test
        @DisplayName("throws when rate limit key exists")
        void rateLimit() {
            when(redisTemplate.hasKey(anyString())).thenReturn(true);

            assertThatThrownBy(() -> otpService.create(PURPOSE, SUBJECT, OtpOptions.defaults()))
                    .isInstanceOf(AppException.class)
                    .hasMessageContaining("wait");
        }
    }

    @Nested
    @DisplayName("verifyAndConsume")
    class VerifyAndConsume {

        @Test
        @DisplayName("returns false when code is null")
        void nullCode() {
            boolean ok = otpService.verifyAndConsume(PURPOSE, SUBJECT, null);
            assertThat(ok).isFalse();
        }

        @Test
        @DisplayName("returns false when no OTP stored")
        void noStoredOtp() {
            when(redisTemplate.opsForValue()).thenReturn(valueOps);
            when(properties.getMaxVerifyAttempts()).thenReturn(5);
            when(valueOps.get(anyString())).thenReturn(null);

            boolean ok = otpService.verifyAndConsume(PURPOSE, SUBJECT, "123456");
            assertThat(ok).isFalse();
        }

        @Test
        @DisplayName("returns false when code does not match")
        void wrongCode() {
            when(redisTemplate.opsForValue()).thenReturn(valueOps);
            when(properties.getMaxVerifyAttempts()).thenReturn(5);
            // Stored hash is for a different code
            when(valueOps.get(anyString())).thenAnswer(inv -> {
                String key = inv.getArgument(0);
                if (key != null && key.contains("otp:data:")) {
                    return "a1b2c3d4e5f6"; // any non-matching hash
                }
                return "0";
            });

            boolean ok = otpService.verifyAndConsume(PURPOSE, SUBJECT, "000000");
            assertThat(ok).isFalse();
        }

        @Test
        @DisplayName("returns true and consumes when code matches")
        void success() {
            when(redisTemplate.opsForValue()).thenReturn(valueOps);
            when(redisTemplate.delete(anyString())).thenReturn(Boolean.TRUE);
            when(properties.getMaxVerifyAttempts()).thenReturn(5);
            // SHA-256("123456") in hex — stored hash returned for data key; null for attempts key
            String dataKeyPrefix = "otp:data:" + PURPOSE + ":";
            String attemptsKeyPrefix = "otp:attempts:" + PURPOSE + ":";
            when(valueOps.get(anyString())).thenAnswer(inv -> {
                String key = inv.getArgument(0);
                if (key != null && key.startsWith(dataKeyPrefix)) {
                    return "8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92";
                }
                if (key != null && key.startsWith(attemptsKeyPrefix)) {
                    return null;
                }
                return null;
            });

            boolean ok = otpService.verifyAndConsume(PURPOSE, SUBJECT, "123456");
            assertThat(ok).isTrue();
        }
    }

    @Nested
    @DisplayName("isOperational")
    class IsOperational {

        @Test
        @DisplayName("returns true when Redis responds to ping")
        void whenRedisAvailable() {
            var factory = org.mockito.Mockito.mock(org.springframework.data.redis.connection.RedisConnectionFactory.class);
            var conn = org.mockito.Mockito.mock(org.springframework.data.redis.connection.RedisConnection.class);
            when(redisTemplate.getConnectionFactory()).thenReturn(factory);
            when(factory.getConnection()).thenReturn(conn);
            when(conn.ping()).thenReturn("PONG");

            assertThat(otpService.isOperational()).isTrue();
        }
    }
}
