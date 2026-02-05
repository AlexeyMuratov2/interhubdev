package com.example.interhubdev.auth.internal;

import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks failed login attempts per IP and blocks further attempts when limit is exceeded.
 * Uses configurable sliding window; expired entries are cleaned up periodically.
 */
@Service
class LoginRateLimitService {

    private static final long CLEANUP_INTERVAL_MS = 60_000L;

    private final AuthProperties authProperties;
    private final ConcurrentHashMap<String, AttemptWindow> attemptsByIp = new ConcurrentHashMap<>();
    private volatile long lastCleanupMs = 0;

    LoginRateLimitService(AuthProperties authProperties) {
        this.authProperties = authProperties;
    }

    /**
     * Returns true if the client IP is allowed to attempt login (under limit).
     * Call this at the start of login flow.
     */
    public boolean tryAcquire(String clientIp) {
        maybeCleanup();
        AttemptWindow w = attemptsByIp.get(clientIp);
        if (w == null) {
            return true;
        }
        long now = System.currentTimeMillis();
        if (now - w.windowStartMs >= windowMs()) {
            attemptsByIp.remove(clientIp);
            return true;
        }
        return w.count < authProperties.getLoginRateLimit().getMaxAttempts();
    }

    /**
     * Records a failed login attempt for the client IP.
     * Call this before throwing on invalid credentials / user not active / disabled.
     */
    public void recordFailedAttempt(String clientIp) {
        long now = System.currentTimeMillis();
        long wMs = windowMs();
        attemptsByIp.compute(clientIp, (k, w) -> {
            if (w == null || now - w.windowStartMs >= wMs) {
                return new AttemptWindow(1, now);
            }
            w.count++;
            return w;
        });
    }

    private long windowMs() {
        return authProperties.getLoginRateLimit().getWindowMinutes() * 60L * 1000L;
    }

    private void maybeCleanup() {
        long now = System.currentTimeMillis();
        if (now - lastCleanupMs < CLEANUP_INTERVAL_MS) {
            return;
        }
        synchronized (this) {
            if (now - lastCleanupMs < CLEANUP_INTERVAL_MS) {
                return;
            }
            lastCleanupMs = now;
        }
        long cutoff = now - windowMs();
        attemptsByIp.entrySet().removeIf(e -> e.getValue().windowStartMs < cutoff);
    }

    private static class AttemptWindow {
        int count;
        final long windowStartMs;

        AttemptWindow(int count, long windowStartMs) {
            this.count = count;
            this.windowStartMs = windowStartMs;
        }
    }
}
