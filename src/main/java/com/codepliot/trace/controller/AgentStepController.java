package com.codepliot.trace.controller;

import com.codepliot.common.result.Result;
import com.codepliot.trace.service.AgentStepService;
import com.codepliot.trace.vo.AgentStepVO;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Agent Step 查询接口。
 */
@RestController
@RequestMapping("/api/tasks/{taskId}/steps")
public class AgentStepController {

    private final AgentStepService agentStepService;

    public AgentStepController(AgentStepService agentStepService) {
        this.agentStepService = agentStepService;
    }

    /**
     * 查询当前用户某个任务下的全部执行步骤。
     */
    @GetMapping
    public Result<List<AgentStepVO>> list(@PathVariable Long taskId) {
        return Result.success(agentStepService.listTaskSteps(taskId));
    }
}
