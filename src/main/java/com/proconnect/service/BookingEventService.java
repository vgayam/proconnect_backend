package com.proconnect.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.proconnect.dto.BookingDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Manages SSE emitters for real-time booking push notifications.
 * One list of emitters per professional ID.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BookingEventService {

    // professionalId -> list of active SSE connections
    private final Map<Long, List<SseEmitter>> emitters = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper;

    /** Subscribe a new SSE connection for a professional. */
    public SseEmitter subscribe(Long professionalId) {
        // 0L = no timeout (Render will close idle connections after ~30s anyway;
        // the client reconnects automatically via EventSource)
        SseEmitter emitter = new SseEmitter(0L);

        List<SseEmitter> list = emitters.computeIfAbsent(
                professionalId, k -> new CopyOnWriteArrayList<>());
        list.add(emitter);

        Runnable cleanup = () -> {
            list.remove(emitter);
            if (list.isEmpty()) emitters.remove(professionalId);
            log.debug("SSE emitter removed for professional {}", professionalId);
        };
        emitter.onCompletion(cleanup);
        emitter.onTimeout(cleanup);
        emitter.onError(e -> cleanup.run());

        // Send a "connected" heartbeat immediately so the client knows it's live
        try {
            emitter.send(SseEmitter.event()
                    .name("connected")
                    .data("{\"status\":\"connected\"}"));
        } catch (IOException e) {
            cleanup.run();
        }

        log.debug("SSE emitter registered for professional {}", professionalId);
        return emitter;
    }

    /** Push a new booking event to all active connections for a professional. */
    public void pushNewBooking(Long professionalId, BookingDTO booking) {
        List<SseEmitter> list = emitters.get(professionalId);
        if (list == null || list.isEmpty()) return;

        String json;
        try {
            json = objectMapper.writeValueAsString(booking);
        } catch (IOException e) {
            log.error("Failed to serialize BookingDTO for SSE push", e);
            return;
        }

        List<SseEmitter> dead = new CopyOnWriteArrayList<>();
        for (SseEmitter emitter : list) {
            try {
                emitter.send(SseEmitter.event()
                        .name("new-booking")
                        .data(json));
            } catch (IOException e) {
                dead.add(emitter);
            }
        }
        list.removeAll(dead);
        log.info("Pushed new-booking SSE event to {} connection(s) for professional {}",
                list.size(), professionalId);
    }
}
