package com.codepliot.controller;

import com.codepliot.model.PatchRecordVO;
import com.codepliot.model.PatchReviewRecordVO;
import com.codepliot.model.PullRequestSubmitResult;
import com.codepliot.model.Result;
import com.codepliot.service.patch.PatchReviewRecordService;
import com.codepliot.service.patch.PatchService;
import com.codepliot.service.patch.PullRequestSubmitService;
import com.codepliot.service.task.AgentTaskService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 补丁记录控制器
 * <p>
 * 提供 Agent 任务关联的补丁记录管理接口，包括查看补丁详情、
 * 获取 AI 补丁评审记录以及提交 Pull Request 等操作。
 * </p>
 */
@RestController
@RequestMapping("/api/tasks/{taskId}/patch")
public class PatchRecordController {

    private final PatchService patchService;
    private final PullRequestSubmitService pullRequestSubmitService;
    private final PatchReviewRecordService patchReviewRecordService;
    private final AgentTaskService agentTaskService;

    public PatchRecordController(PatchService patchService,
                                 PullRequestSubmitService pullRequestSubmitService,
                                 PatchReviewRecordService patchReviewRecordService,
                                 AgentTaskService agentTaskService) {
        this.patchService = patchService;
        this.pullRequestSubmitService = pullRequestSubmitService;
        this.patchReviewRecordService = patchReviewRecordService;
        this.agentTaskService = agentTaskService;
    }

    /**
     * 获取指定任务的补丁详情
     *
     * @param taskId 任务 ID
     * @return 补丁记录视图对象
     */
    @GetMapping
    public Result<PatchRecordVO> detail(@PathVariable Long taskId) {
        return Result.success(patchService.getTaskPatch(taskId));
    }

    /**
     * 获取指定任务的最新 AI 补丁评审记录
     *
     * @param taskId 任务 ID
     * @return 补丁评审记录视图对象
     */
    @GetMapping("/review")
    public Result<PatchReviewRecordVO> review(@PathVariable Long taskId) {
        agentTaskService.getOwnedTaskEntity(taskId);
        return Result.success(patchReviewRecordService.getLatestByTaskId(taskId));
    }

    /**
     * 将指定任务的补丁提交为 GitHub Pull Request
     *
     * @param taskId 任务 ID
     * @return PR 提交结果
     */
    @PostMapping("/pull-request")
    public Result<PullRequestSubmitResult> submitPullRequest(@PathVariable Long taskId) {
        return Result.success("Pull request submitted", pullRequestSubmitService.submit(taskId));
    }
}
