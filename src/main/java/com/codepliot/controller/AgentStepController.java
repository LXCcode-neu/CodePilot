package com.codepliot.controller;

import com.codepliot.model.Result;
import com.codepliot.service.AgentStepService;
import com.codepliot.model.AgentStepVO;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
/**
 * AgentStepController 控制器，负责对外提供 HTTP 接口。
 */
@RestController
@RequestMapping("/api/tasks/{taskId}/steps")
public class AgentStepController {

    private final AgentStepService agentStepService;
/**
 * 创建 AgentStepController 实例。
 */
public AgentStepController(AgentStepService agentStepService) {
        this.agentStepService = agentStepService;
    }
    /**
     * 执行 list 相关逻辑。
     */
@GetMapping
    public Result<List<AgentStepVO>> list(@PathVariable Long taskId) {
        return Result.success(agentStepService.listTaskSteps(taskId));
    }
}

