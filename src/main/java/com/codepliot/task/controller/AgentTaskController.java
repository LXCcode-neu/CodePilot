package com.codepliot.task.controller;

import com.codepliot.agent.service.AgentRunService;
import com.codepliot.common.result.Result;
import com.codepliot.task.dto.AgentTaskCreateRequest;
import com.codepliot.task.service.AgentTaskService;
import com.codepliot.task.vo.AgentTaskVO;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Agent 任务相关接口，只面向当前登录用户。
 */
@RestController
@RequestMapping("/api/tasks")
public class AgentTaskController {

    private final AgentTaskService agentTaskService;
    private final AgentRunService agentRunService;

    public AgentTaskController(AgentTaskService agentTaskService, AgentRunService agentRunService) {
        this.agentTaskService = agentTaskService;
        this.agentRunService = agentRunService;
    }

    /**
     * 基于当前用户自己的项目创建 Agent 任务。
     */
    @PostMapping
    public Result<AgentTaskVO> create(@Valid @RequestBody AgentTaskCreateRequest request) {
        return Result.success("Agent task created", agentTaskService.create(request));
    }

    /**
     * 查询当前用户自己的任务列表。
     */
    @GetMapping
    public Result<List<AgentTaskVO>> list() {
        return Result.success(agentTaskService.listCurrentUserTasks());
    }

    /**
     * 查询当前用户自己的任务详情。
     */
    @GetMapping("/{id}")
    public Result<AgentTaskVO> detail(@PathVariable Long id) {
        return Result.success(agentTaskService.getDetail(id));
    }

    /**
     * 异步触发当前登录用户自己的 Agent 任务。
     */
    @PostMapping("/{taskId}/run")
    public Result<AgentTaskVO> run(@PathVariable Long taskId) {
        return Result.success("Agent task submitted", agentRunService.run(taskId));
    }
}
