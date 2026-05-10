package com.codepliot.service.sse;

import com.codepliot.model.TaskEventMessage;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
/**
 * SseService 服务类，负责封装业务流程和领域能力。
 */
@Service
public class SseService {

    private static final long EMITTER_TIMEOUT_MILLIS = 30L * 60L * 1000L;

    private final Map<Long, CopyOnWriteArrayList<SseEmitter>> taskEmitters = new ConcurrentHashMap<>();
/**
 * 执行 subscribe 相关逻辑。
 */
public SseEmitter subscribe(Long taskId) {
        SseEmitter emitter = new SseEmitter(EMITTER_TIMEOUT_MILLIS);
        taskEmitters.computeIfAbsent(taskId, ignored -> new CopyOnWriteArrayList<>()).add(emitter);

        emitter.onCompletion(() -> removeEmitter(taskId, emitter));
        emitter.onTimeout(() -> {
            emitter.complete();
            removeEmitter(taskId, emitter);
        });
        emitter.onError(ignored -> {
            emitter.complete();
            removeEmitter(taskId, emitter);
        });
        return emitter;
    }
/**
 * 执行 send 相关逻辑。
 */
public void send(SseEmitter emitter, TaskEventMessage message) throws IOException {
        emitter.send(SseEmitter.event()
                .name("task-event")
                .data(message));
    }
/**
 * 执行 push 相关逻辑。
 */
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
/**
 * 执行 complete 相关逻辑。
 */
public void complete(Long taskId) {
        List<SseEmitter> emitters = taskEmitters.remove(taskId);
        if (emitters == null || emitters.isEmpty()) {
            return;
        }
        for (SseEmitter emitter : emitters) {
            try {
                emitter.complete();
            } catch (IllegalStateException ignored) {
                // 忽略已经完成的 emitter。
            }
        }
    }
/**
 * 移除Emitter相关逻辑。
 */
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
