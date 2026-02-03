package com.example.interhubdev.auth.internal;

import com.example.interhubdev.error.ErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.core.Ordered;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Deque;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * Rate limit filter: returns 429 Too Many Requests when a client exceeds
 * the allowed number of requests per second (5 per second per client IP).
 */
@Slf4j
class RateLimitFilter extends OncePerRequestFilter implements Ordered {

    private static final int ORDER = -100;
    private static final int MAX_REQUESTS_PER_SECOND = 5;
    private static final long WINDOW_MS = 1000L;

    private final ConcurrentHashMap<String, Deque<Long>> clientTimestamps = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        String clientKey = resolveClientKey(request);
        long now = System.currentTimeMillis();

        Deque<Long> timestamps = clientTimestamps.computeIfAbsent(clientKey, k -> new ConcurrentLinkedDeque<>());

        synchronized (timestamps) {
            // Remove timestamps outside the current 1-second window
            while (!timestamps.isEmpty() && now - timestamps.peekFirst() > WINDOW_MS) {
                timestamps.pollFirst();
            }
            if (timestamps.size() >= MAX_REQUESTS_PER_SECOND) {
                response.setStatus(429); // Too Many Requests
                response.setContentType("application/json");
                response.setCharacterEncoding("UTF-8");
                response.setHeader("Retry-After", "1");
                ErrorResponse body = ErrorResponse.of(
                        "TOO_MANY_REQUESTS",
                        "Превышен лимит запросов. Максимум " + MAX_REQUESTS_PER_SECOND + " запросов в секунду.");
                objectMapper.writeValue(response.getOutputStream(), body);
                log.debug("Rate limit exceeded for client: {}", clientKey);
                return;
            }
            timestamps.addLast(now);
        }

        filterChain.doFilter(request, response);
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
