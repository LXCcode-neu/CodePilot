package com.codepliot.controller;

import com.codepliot.model.AgentTaskCreateRequest;
import com.codepliot.model.AgentTaskVO;
import com.codepliot.model.Result;
import com.codepliot.service.agent.AgentRunService;
import com.codepliot.service.patch.PatchService;
import com.codepliot.service.task.AgentTaskService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tasks")
public class AgentTaskController {

    private final AgentTaskService agentTaskService;
    private final AgentRunService agentRunService;
    private final PatchService patchService;

    public AgentTaskController(AgentTaskService agentTaskService,
                               AgentRunService agentRunService,
                               PatchService patchService) {
        this.agentTaskService = agentTaskService;
        this.agentRunService = agentRunService;
        this.patchService = patchService;
    }

    @PostMapping
    public Result<AgentTaskVO> create(@Valid @RequestBody AgentTaskCreateRequest request) {
        return Result.success("Agent task created", agentTaskService.create(request));
    }

    @GetMapping
    public Result<List<AgentTaskVO>> list() {
        return Result.success(agentTaskService.listCurrentUserTasks());
    }

    @GetMapping("/{id}")
    public Result<AgentTaskVO> detail(@PathVariable Long id) {
        return Result.success(agentTaskService.getDetail(id));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        agentTaskService.delete(id);
        return Result.success("Agent task deleted", null);
    }

    @PostMapping("/{taskId}/run")
    public Result<AgentTaskVO> run(@PathVariable Long taskId) {
        return Result.success("Agent task submitted", agentRunService.run(taskId));
    }

    @PostMapping("/{taskId}/confirm")
    public Result<AgentTaskVO> confirm(@PathVariable Long taskId) {
        return Result.success("Patch confirmed", patchService.confirmTaskPatch(taskId));
    }
}
