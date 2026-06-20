package com.codepliot.controller;

import com.codepliot.model.AgentTaskCreateRequest;
import com.codepliot.model.AgentTaskVO;
import com.codepliot.model.Result;
import com.codepliot.service.agent.AgentRunService;
import com.codepliot.service.agent.AgentTaskCancellationService;
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

/**
 * Agent 任务控制器
 * <p>
 * 提供 Agent 任务的完整生命周期管理接口，包括任务的创建、查询、删除、运行、取消以及补丁确认等操作。
 * </p>
 */
@RestController
@RequestMapping("/api/tasks")
public class AgentTaskController {

    private final AgentTaskService agentTaskService;
    private final AgentRunService agentRunService;
    private final AgentTaskCancellationService agentTaskCancellationService;
    private final PatchService patchService;

    public AgentTaskController(AgentTaskService agentTaskService,
                               AgentRunService agentRunService,
                               AgentTaskCancellationService agentTaskCancellationService,
                               PatchService patchService) {
        this.agentTaskService = agentTaskService;
        this.agentRunService = agentRunService;
        this.agentTaskCancellationService = agentTaskCancellationService;
        this.patchService = patchService;
    }

    /**
     * 创建 Agent 任务
     *
     * @param request 任务创建请求参数，包含任务的基本配置信息
     * @return 创建成功后的任务视图对象
     */
    @PostMapping
    public Result<AgentTaskVO> create(@Valid @RequestBody AgentTaskCreateRequest request) {
        return Result.success("Agent task created", agentTaskService.create(request));
    }

    /**
     * 获取当前用户的所有 Agent 任务列表
     *
     * @return 当前用户的任务列表
     */
    @GetMapping
    public Result<List<AgentTaskVO>> list() {
        return Result.success(agentTaskService.listCurrentUserTasks());
    }

    /**
     * 获取指定 Agent 任务的详细信息
     *
     * @param id 任务 ID
     * @return 任务详情视图对象
     */
    @GetMapping("/{id}")
    public Result<AgentTaskVO> detail(@PathVariable Long id) {
        return Result.success(agentTaskService.getDetail(id));
    }

    /**
     * 删除指定的 Agent 任务
     *
     * @param id 任务 ID
     * @return 操作结果
     */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        agentTaskService.delete(id);
        return Result.success("Agent task deleted", null);
    }

    /**
     * 提交运行指定的 Agent 任务
     *
     * @param taskId 任务 ID
     * @return 运行后的任务视图对象
     */
    @PostMapping("/{taskId}/run")
    public Result<AgentTaskVO> run(@PathVariable Long taskId) {
        return Result.success("Agent task submitted", agentRunService.run(taskId));
    }

    /**
     * 请求取消正在运行的 Agent 任务
     *
     * @param taskId 任务 ID
     * @return 取消请求处理后的任务视图对象
     */
    @PostMapping("/{taskId}/cancel")
    public Result<AgentTaskVO> cancel(@PathVariable Long taskId) {
        return Result.success("Agent task cancellation requested", agentTaskCancellationService.requestCancel(taskId));
    }

    /**
     * 确认指定任务生成的补丁
     *
     * @param taskId 任务 ID
     * @return 确认后的任务视图对象
     */
    @PostMapping("/{taskId}/confirm")
    public Result<AgentTaskVO> confirm(@PathVariable Long taskId) {
        return Result.success("Patch confirmed", patchService.confirmTaskPatch(taskId));
    }
}
