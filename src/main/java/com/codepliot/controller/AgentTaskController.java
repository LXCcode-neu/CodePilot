package com.codepliot.controller;

import com.codepliot.model.AgentTaskCreateRequest;
import com.codepliot.model.AgentTaskVO;
import com.codepliot.model.Result;
import com.codepliot.service.agent.AgentRunService;
import com.codepliot.service.task.AgentTaskService;
import com.codepliot.service.patch.PatchService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Agent 任务控制器。
 */
@RestController
@RequestMapping("/api/tasks")
public class AgentTaskController {

    private final AgentTaskService agentTaskService;
    private final AgentRunService agentRunService;
    private final PatchService patchService;

    /**
     * 创建 Agent 任务控制器。
     */
    public AgentTaskController(AgentTaskService agentTaskService,
                               AgentRunService agentRunService,
                               PatchService patchService) {
        this.agentTaskService = agentTaskService;
        this.agentRunService = agentRunService;
        this.patchService = patchService;
    }

    /**
     * 创建任务。
     */
    @PostMapping
    public Result<AgentTaskVO> create(@Valid @RequestBody AgentTaskCreateRequest request) {
        return Result.success("Agent task created", agentTaskService.create(request));
    }

    /**
     * 查询当前用户任务列表。
     */
    @GetMapping
    public Result<List<AgentTaskVO>> list() {
        return Result.success(agentTaskService.listCurrentUserTasks());
    }

    /**
     * 查询任务详情。
     */
    @GetMapping("/{id}")
    public Result<AgentTaskVO> detail(@PathVariable Long id) {
        return Result.success(agentTaskService.getDetail(id));
    }

    /**
     * 触发任务运行。
     */
    @PostMapping("/{taskId}/run")
    public Result<AgentTaskVO> run(@PathVariable Long taskId) {
        return Result.success("Agent task submitted", agentRunService.run(taskId));
    }

    /**
     * 人工确认当前任务的 patch 结果。
     */
    @PostMapping("/{taskId}/confirm")
    public Result<AgentTaskVO> confirm(@PathVariable Long taskId) {
        return Result.success("Patch confirmed", patchService.confirmTaskPatch(taskId));
    }
}
