package com.codepliot.controller;

import com.codepliot.model.AgentTaskVO;
import com.codepliot.model.GitHubIssuePageVO;
import com.codepliot.model.Result;
import com.codepliot.service.githubIssue.GitHubIssueService;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * GitHub Issue 控制器
 * <p>
 * 提供项目维度的 GitHub Issue 查询与导入功能，支持分页列表查询
 * 以及将 Issue 导入为 Agent 任务以便自动化处理。
 * </p>
 */
@RestController
@RequestMapping("/api/projects/{projectId}/github/issues")
public class GitHubIssueController {

    private final GitHubIssueService gitHubIssueService;

    public GitHubIssueController(GitHubIssueService gitHubIssueService) {
        this.gitHubIssueService = gitHubIssueService;
    }

    /**
     * 分页查询项目关联的 GitHub Issue 列表
     *
     * @param projectId 项目 ID
     * @param state     Issue 状态筛选条件，默认为 "all"（可选值：all、open、closed）
     * @param page      页码，默认为 1
     * @param pageSize  每页条数，默认为 10
     * @return Issue 分页结果
     */
    @GetMapping
    public Result<GitHubIssuePageVO> list(@PathVariable Long projectId,
                                          @RequestParam(defaultValue = "all") String state,
                                          @RequestParam(defaultValue = "1") Integer page,
                                          @RequestParam(defaultValue = "10") Integer pageSize) {
        return Result.success(gitHubIssueService.listIssues(projectId, state, page, pageSize));
    }

    /**
     * 将指定 GitHub Issue 导入为 Agent 任务
     *
     * @param projectId   项目 ID
     * @param issueNumber Issue 编号
     * @return 导入成功后的 Agent 任务视图对象
     */
    @PostMapping("/{issueNumber}/import-task")
    public Result<AgentTaskVO> importTask(@PathVariable Long projectId, @PathVariable Integer issueNumber) {
        return Result.success("GitHub issue imported as agent task", gitHubIssueService.importIssueAsTask(projectId, issueNumber));
    }
}
