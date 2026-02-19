package com.example.interhubdev.outbox.internal;

import com.example.interhubdev.outbox.OutboxEventHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Registry of outbox event handlers.
 * Automatically discovers all OutboxEventHandler beans and registers them by event type.
 * Package-private: only accessible within the outbox module.
 */
@Component
@Slf4j
class OutboxHandlerRegistry {

    private final Map<String, OutboxEventHandler> handlers = new HashMap<>();

    @Autowired(required = false)
    private List<OutboxEventHandler> handlerBeans = List.of();

    @PostConstruct
    void registerHandlers() {
        for (OutboxEventHandler handler : handlerBeans) {
            String eventType = handler.eventType();
            if (handlers.containsKey(eventType)) {
                log.warn("Duplicate handler for event type '{}': {} and {}", 
                        eventType, handlers.get(eventType).getClass().getName(), handler.getClass().getName());
            }
            handlers.put(eventType, handler);
            log.info("Registered outbox handler: {} -> {}", eventType, handler.getClass().getName());
        }
        log.info("Registered {} outbox event handler(s)", handlers.size());
    }

    /**
     * Get handler for event type.
     *
     * @param eventType event type identifier
     * @return optional handler, empty if no handler registered
     */
    Optional<OutboxEventHandler> getHandler(String eventType) {
        return Optional.ofNullable(handlers.get(eventType));
    }

    /**
     * Check if handler exists for event type.
     *
     * @param eventType event type identifier
     * @return true if handler is registered
     */
    boolean hasHandler(String eventType) {
        return handlers.containsKey(eventType);
    }
}
