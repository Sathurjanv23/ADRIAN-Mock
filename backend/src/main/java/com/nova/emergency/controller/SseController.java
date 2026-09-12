package com.nova.emergency.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.List;

/**
 * SseController — Server-Sent Events endpoint for real-time emergency updates.
 *
 * Authenticated clients connect to: GET /api/events/stream
 * The server pushes events when incidents are created or updated.
 *
 * Event types:
 *   - incident_created : new emergency incident
 *   - incident_updated : existing incident status/data changed
 *   - alert            : live system alert
 *   - heartbeat        : keep-alive ping (every 30s)
 */
@RestController
@RequestMapping("/api/events")
public class SseController {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(SseController.class);

    private static final long SSE_TIMEOUT_MS = 5 * 60 * 1000L; // 5 minutes

    /**
     * Thread-safe list of active SSE emitters.
     * CopyOnWriteArrayList ensures safe concurrent iteration during broadcast.
     */
    private static final List<SseEmitter> emitters = new CopyOnWriteArrayList<>();

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Authenticated endpoint: clients subscribe to this stream to receive live updates.
     * JWT is required (enforced by Spring Security filter chain).
     */
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream() {
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT_MS);
        emitters.add(emitter);

        emitter.onCompletion(() -> {
            emitters.remove(emitter);
            log.debug("SSE client disconnected. Active connections: {}", emitters.size());
        });
        emitter.onTimeout(() -> {
            emitters.remove(emitter);
            log.debug("SSE client timed out. Active connections: {}", emitters.size());
        });
        emitter.onError((e) -> {
            emitters.remove(emitter);
            log.debug("SSE client error: {}. Active connections: {}", e.getMessage(), emitters.size());
        });

        // Send initial heartbeat to confirm connection
        try {
            emitter.send(SseEmitter.event()
                .name("heartbeat")
                .data("{\"status\":\"connected\",\"timestamp\":\"" + java.time.Instant.now() + "\"}"));
        } catch (IOException e) {
            emitters.remove(emitter);
        }

        log.debug("New SSE client connected. Active connections: {}", emitters.size());
        return emitter;
    }

    // ─── Broadcast Helpers (called by services) ────────────────

    /**
     * Broadcasts an incident_created event to all connected SSE clients.
     */
    public static void broadcastIncidentCreated(Object incident) {
        broadcastEvent("incident_created", incident);
    }

    /**
     * Broadcasts an incident_updated event to all connected SSE clients.
     */
    public static void broadcastIncidentUpdated(Object incident) {
        broadcastEvent("incident_updated", incident);
    }

    /**
     * Broadcasts a live alert event to all connected SSE clients.
     */
    public static void broadcastAlert(Map<String, Object> alert) {
        broadcastEvent("alert", alert);
    }

    /**
     * Generic broadcast method for ADRN relief logistics and other events.
     * Supports any event type (e.g., RELIEF_LOCATION_UPDATED, CRITICAL_SHORTAGE_DETECTED).
     * Can be called as instance method by Spring beans (e.g., ReliefScheduler).
     */
    public void broadcast(String eventType, Object data) {
        broadcastEvent(eventType, data);
    }

    /**
     * Static version for non-Spring callers.
     */
    public static void broadcastStatic(String eventType, Object data) {
        broadcastEvent(eventType, data);
    }

    // ─── Internal Broadcast ────────────────────────────────────

    private static void broadcastEvent(String eventName, Object data) {
        ObjectMapper mapper = new ObjectMapper();
        List<SseEmitter> deadEmitters = new CopyOnWriteArrayList<>();

        for (SseEmitter emitter : emitters) {
            try {
                String json = mapper.writeValueAsString(data);
                emitter.send(SseEmitter.event().name(eventName).data(json));
            } catch (IOException e) {
                deadEmitters.add(emitter);
            }
        }

        // Remove disconnected emitters
        emitters.removeAll(deadEmitters);
    }
}
