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

    @GetMapping
    public Result<PatchRecordVO> detail(@PathVariable Long taskId) {
        return Result.success(patchService.getTaskPatch(taskId));
    }

    @GetMapping("/review")
    public Result<PatchReviewRecordVO> review(@PathVariable Long taskId) {
        agentTaskService.getOwnedTaskEntity(taskId);
        return Result.success(patchReviewRecordService.getLatestByTaskId(taskId));
    }

    @PostMapping("/pull-request")
    public Result<PullRequestSubmitResult> submitPullRequest(@PathVariable Long taskId) {
        return Result.success("Pull request submitted", pullRequestSubmitService.submit(taskId));
    }
}
