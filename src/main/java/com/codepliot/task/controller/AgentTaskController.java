package com.codepliot.task.controller;

import com.codepliot.common.result.Result;
import com.codepliot.task.dto.AgentTaskCreateRequest;
import com.codepliot.task.service.AgentStepService;
import com.codepliot.task.service.AgentTaskService;
import com.codepliot.task.vo.AgentStepVO;
import com.codepliot.task.vo.AgentTaskVO;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tasks")
/**
 * Agent 任务相关接口，只面向当前登录用户。
 */
public class AgentTaskController {

    private final AgentTaskService agentTaskService;
    private final AgentStepService agentStepService;

    public AgentTaskController(AgentTaskService agentTaskService, AgentStepService agentStepService) {
        this.agentTaskService = agentTaskService;
        this.agentStepService = agentStepService;
    }

    @PostMapping
    /**
     * 基于当前用户自己的项目创建 Agent 任务。
     */
    public Result<AgentTaskVO> create(@Valid @RequestBody AgentTaskCreateRequest request) {
        return Result.success("Agent task created", agentTaskService.create(request));
    }

    @GetMapping
    /**
     * 查询当前用户自己的任务列表。
     */
    public Result<List<AgentTaskVO>> list() {
        return Result.success(agentTaskService.listCurrentUserTasks());
    }

    @GetMapping("/{id}")
    /**
     * 查询当前用户自己的任务详情。
     */
    public Result<AgentTaskVO> detail(@PathVariable Long id) {
        return Result.success(agentTaskService.getDetail(id));
    }

    /**
     * 查询当前用户某个任务下的所有执行步骤。
     */
    @GetMapping("/{taskId}/steps")
    public Result<List<AgentStepVO>> steps(@PathVariable Long taskId) {
        return Result.success(agentStepService.listTaskSteps(taskId));
    }
}
