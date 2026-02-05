package com.example.interhubdev.auth.internal;

import com.example.interhubdev.error.ErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * Rate limit filter: returns 429 Too Many Requests when a client exceeds
 * the configured requests per window. Client key is IP (or X-Forwarded-For).
 * Periodically cleans up inactive keys to bound memory use.
 */
@Slf4j
class RateLimitFilter extends OncePerRequestFilter implements Ordered {

    private static final int ORDER = -100;

    private final AuthProperties authProperties;
    private final ConcurrentHashMap<String, Deque<Long>> clientTimestamps = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    private volatile long lastCleanupMs = 0;

    RateLimitFilter(AuthProperties authProperties) {
        this.authProperties = authProperties;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        String clientKey = resolveClientKey(request);
        long now = System.currentTimeMillis();

        maybeCleanup(now);

        int maxRequests = authProperties.getRateLimit().getMaxRequestsPerSecond();
        long windowMs = authProperties.getRateLimit().getWindowMs();

        Deque<Long> timestamps = clientTimestamps.computeIfAbsent(clientKey, k -> new ConcurrentLinkedDeque<>());

        synchronized (timestamps) {
            while (!timestamps.isEmpty() && now - timestamps.peekFirst() > windowMs) {
                timestamps.pollFirst();
            }
            if (timestamps.size() >= maxRequests) {
                response.setStatus(429);
                response.setContentType("application/json");
                response.setCharacterEncoding("UTF-8");
                response.setHeader("Retry-After", "1");
                ErrorResponse body = ErrorResponse.of(
                        "TOO_MANY_REQUESTS",
                        "Превышен лимит запросов. Максимум " + maxRequests + " запросов в секунду.");
                objectMapper.writeValue(response.getOutputStream(), body);
                log.debug("Rate limit exceeded for client: {}", clientKey);
                return;
            }
            timestamps.addLast(now);
        }

        filterChain.doFilter(request, response);
    }

    private void maybeCleanup(long now) {
        long cleanupAfter = authProperties.getRateLimit().getCleanupAfterInactiveMs();
        if (now - lastCleanupMs < cleanupAfter) {
            return;
        }
        synchronized (this) {
            if (now - lastCleanupMs < cleanupAfter) {
                return;
            }
            lastCleanupMs = now;
        }
        List<String> toRemove = new ArrayList<>();
        clientTimestamps.forEach((key, deque) -> {
            synchronized (deque) {
                if (deque.isEmpty() || now - deque.peekLast() > cleanupAfter) {
                    toRemove.add(key);
                }
            }
        });
        toRemove.forEach(clientTimestamps::remove);
    }

    private static String resolveClientKey(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr() != null ? request.getRemoteAddr() : "unknown";
    }

    @Override
    public int getOrder() {
        return ORDER;
    }
}
