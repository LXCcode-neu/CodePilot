package com.codepliot.controller;

import com.codepliot.service.AgentRunService;
import com.codepliot.model.Result;
import com.codepliot.model.AgentTaskCreateRequest;
import com.codepliot.service.AgentTaskService;
import com.codepliot.model.AgentTaskVO;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
/**
 * AgentTaskController 控制器，负责对外提供 HTTP 接口。
 */
@RestController
@RequestMapping("/api/tasks")
public class AgentTaskController {

    private final AgentTaskService agentTaskService;
    private final AgentRunService agentRunService;
/**
 * 创建 AgentTaskController 实例。
 */
public AgentTaskController(AgentTaskService agentTaskService, AgentRunService agentRunService) {
        this.agentTaskService = agentTaskService;
        this.agentRunService = agentRunService;
    }
    /**
     * 执行 create 相关逻辑。
     */
@PostMapping
    public Result<AgentTaskVO> create(@Valid @RequestBody AgentTaskCreateRequest request) {
        return Result.success("Agent task created", agentTaskService.create(request));
    }
    /**
     * 执行 list 相关逻辑。
     */
@GetMapping
    public Result<List<AgentTaskVO>> list() {
        return Result.success(agentTaskService.listCurrentUserTasks());
    }
    /**
     * 执行 detail 相关逻辑。
     */
@GetMapping("/{id}")
    public Result<AgentTaskVO> detail(@PathVariable Long id) {
        return Result.success(agentTaskService.getDetail(id));
    }
    /**
     * 执行 run 相关逻辑。
     */
@PostMapping("/{taskId}/run")
    public Result<AgentTaskVO> run(@PathVariable Long taskId) {
        return Result.success("Agent task submitted", agentRunService.run(taskId));
    }
}

