package com.codepliot.controller;

import com.codepliot.model.TaskEventMessage;
import com.codepliot.service.SseService;
import com.codepliot.entity.AgentTask;
import com.codepliot.service.AgentTaskService;
import java.io.IOException;
import java.time.LocalDateTime;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
/**
 * TaskEventController 控制器，负责对外提供 HTTP 接口。
 */
@RestController
@RequestMapping("/api/tasks/{taskId}/events")
public class TaskEventController {

    private final AgentTaskService agentTaskService;
    private final SseService sseService;
/**
 * 创建 TaskEventController 实例。
 */
public TaskEventController(AgentTaskService agentTaskService, SseService sseService) {
        this.agentTaskService = agentTaskService;
        this.sseService = sseService;
    }
    /**
     * 执行 subscribe 相关逻辑。
     */
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
                    "褰撳墠浠诲姟鐘舵€侊細" + agentTask.getStatus(),
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
/**
 * 执行 isTerminalStatus 相关逻辑。
 */
private boolean isTerminalStatus(String status) {
        return "COMPLETED".equals(status) || "FAILED".equals(status);
    }
/**
 * 解析并返回Phase相关逻辑。
 */
private String resolvePhase(String status) {
        if ("COMPLETED".equals(status) || "FAILED".equals(status)) {
            return "COMPLETED";
        }
        if ("PENDING".equals(status)) {
            return "PENDING";
        }
        return "RUNNING";
    }
}

