package com.codepliot.controller;

import com.codepliot.entity.AgentTask;
import com.codepliot.model.TaskEventMessage;
import com.codepliot.service.sse.SseService;
import com.codepliot.service.task.AgentTaskService;
import java.io.IOException;
import java.time.LocalDateTime;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/tasks/{taskId}/events")
public class TaskEventController {

    private final AgentTaskService agentTaskService;
    private final SseService sseService;

    public TaskEventController(AgentTaskService agentTaskService, SseService sseService) {
        this.agentTaskService = agentTaskService;
        this.sseService = sseService;
    }

    @GetMapping(produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribe(@PathVariable Long taskId) {
        AgentTask agentTask = agentTaskService.getOwnedTaskEntity(taskId);
        SseEmitter emitter = sseService.subscribe(taskId);
        try {
            sseService.send(emitter, new TaskEventMessage(
                    agentTask.getId(),
                    agentTask.getStatus(),
                    resolvePhase(agentTask.getStatus()),
                    null,
                    "当前任务状态：" + agentTask.getStatus(),
                    LocalDateTime.now()
            ));
            if (isTerminalStatus(agentTask.getStatus())) {
                emitter.complete();
            }
        } catch (IOException exception) {
            emitter.completeWithError(exception);
        }
        return emitter;
    }

    private boolean isTerminalStatus(String status) {
        return "COMPLETED".equals(status)
                || "FAILED".equals(status)
                || "VERIFY_FAILED".equals(status)
                || "CANCELLED".equals(status);
    }

    private String resolvePhase(String status) {
        if ("COMPLETED".equals(status)
                || "FAILED".equals(status)
                || "VERIFY_FAILED".equals(status)
                || "CANCELLED".equals(status)) {
            return "COMPLETED";
        }
        if ("PENDING".equals(status)) {
            return "PENDING";
        }
        return "RUNNING";
    }
}
