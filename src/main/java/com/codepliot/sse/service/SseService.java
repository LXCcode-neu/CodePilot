package com.codepliot.sse.service;

import com.codepliot.sse.dto.TaskEventMessage;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * SSE 连接管理与事件推送服务。
 */
@Service
public class SseService {

    private static final long EMITTER_TIMEOUT_MILLIS = 30L * 60L * 1000L;

    private final Map<Long, CopyOnWriteArrayList<SseEmitter>> taskEmitters = new ConcurrentHashMap<>();

    public SseEmitter subscribe(Long taskId) {
        SseEmitter emitter = new SseEmitter(EMITTER_TIMEOUT_MILLIS);
        taskEmitters.computeIfAbsent(taskId, ignored -> new CopyOnWriteArrayList<>()).add(emitter);

        emitter.onCompletion(() -> removeEmitter(taskId, emitter));
        emitter.onTimeout(() -> {
            emitter.complete();
            removeEmitter(taskId, emitter);
        });
        emitter.onError(ignored -> removeEmitter(taskId, emitter));
        return emitter;
    }

    public void send(SseEmitter emitter, TaskEventMessage message) throws IOException {
        emitter.send(SseEmitter.event()
                .name("task-event")
                .data(message));
    }

    public void push(TaskEventMessage message) {
        if (message == null || message.taskId() == null) {
            return;
        }

        List<SseEmitter> emitters = taskEmitters.get(message.taskId());
        if (emitters == null || emitters.isEmpty()) {
            return;
        }

        for (SseEmitter emitter : emitters) {
            try {
                send(emitter, message);
            } catch (IOException | IllegalStateException exception) {
                emitter.complete();
                removeEmitter(message.taskId(), emitter);
            }
        }
    }

    public void complete(Long taskId) {
        List<SseEmitter> emitters = taskEmitters.remove(taskId);
        if (emitters == null || emitters.isEmpty()) {
            return;
        }
        for (SseEmitter emitter : emitters) {
            try {
                emitter.complete();
            } catch (IllegalStateException ignored) {
                // Ignore already completed emitters.
            }
        }
    }

    private void removeEmitter(Long taskId, SseEmitter emitter) {
        CopyOnWriteArrayList<SseEmitter> emitters = taskEmitters.get(taskId);
        if (emitters == null) {
            return;
        }
        emitters.remove(emitter);
        if (emitters.isEmpty()) {
            taskEmitters.remove(taskId, emitters);
        }
    }
}
