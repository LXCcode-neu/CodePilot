package com.codepliot.controller;

import com.codepliot.model.GitHubIssueEventRunResult;
import com.codepliot.model.GitHubIssueEventVO;
import com.codepliot.model.Result;
import com.codepliot.service.githubIssue.GitHubIssueEventService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * GitHub Issue 事件控制器
 * <p>
 * 管理由 GitHub Issue Webhook 触发的事件，支持事件列表查询、
 * 忽略事件以及触发自动修复运行等操作。
 * </p>
 */
@RestController
@RequestMapping("/api/issues/events")
public class GitHubIssueEventController {

    private final GitHubIssueEventService gitHubIssueEventService;

    public GitHubIssueEventController(GitHubIssueEventService gitHubIssueEventService) {
        this.gitHubIssueEventService = gitHubIssueEventService;
    }

    /**
     * 分页查询当前用户的 GitHub Issue 事件列表
     *
     * @param status   事件状态筛选条件（可选）
     * @param page     页码，默认为 1
     * @param pageSize 每页条数，默认为 20
     * @return Issue 事件列表
     */
    @GetMapping
    public Result<List<GitHubIssueEventVO>> list(@RequestParam(required = false) String status,
                                                 @RequestParam(defaultValue = "1") Integer page,
                                                 @RequestParam(defaultValue = "20") Integer pageSize) {
        return Result.success(gitHubIssueEventService.listCurrentUserEvents(status, page, pageSize));
    }

    /**
     * 忽略指定的 GitHub Issue 事件
     *
     * @param id 事件 ID
     * @return 更新后的事件视图对象
     */
    @PostMapping("/{id}/ignore")
    public Result<GitHubIssueEventVO> ignore(@PathVariable Long id) {
        return Result.success("GitHub issue event ignored", gitHubIssueEventService.ignore(id));
    }

    /**
     * 触发指定 GitHub Issue 事件的自动修复运行
     *
     * @param id 事件 ID
     * @return 运行结果
     */
    @PostMapping("/{id}/run")
    public Result<GitHubIssueEventRunResult> run(@PathVariable Long id) {
        return Result.success("GitHub issue repair started", gitHubIssueEventService.run(id));
    }
}
