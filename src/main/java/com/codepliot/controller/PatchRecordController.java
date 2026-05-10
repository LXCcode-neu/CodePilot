package com.codepliot.controller;

import com.codepliot.model.Result;
import com.codepliot.model.PullRequestSubmitResult;
import com.codepliot.service.patch.PatchService;
import com.codepliot.service.patch.PullRequestSubmitService;
import com.codepliot.model.PatchRecordVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
/**
 * PatchRecordController 控制器，负责对外提供 HTTP 接口。
 */
@RestController
@RequestMapping("/api/tasks/{taskId}/patch")
public class PatchRecordController {

    private final PatchService patchService;
    private final PullRequestSubmitService pullRequestSubmitService;
/**
 * 创建 PatchRecordController 实例。
 */
public PatchRecordController(PatchService patchService, PullRequestSubmitService pullRequestSubmitService) {
        this.patchService = patchService;
        this.pullRequestSubmitService = pullRequestSubmitService;
    }
    /**
     * 执行 detail 相关逻辑。
     */
@GetMapping
    public Result<PatchRecordVO> detail(@PathVariable Long taskId) {
        return Result.success(patchService.getTaskPatch(taskId));
    }

    @PostMapping("/pull-request")
    public Result<PullRequestSubmitResult> submitPullRequest(@PathVariable Long taskId) {
        return Result.success("Pull request submitted", pullRequestSubmitService.submit(taskId));
    }
}

