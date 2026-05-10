package com.codepliot.controller;

import com.codepliot.model.AgentTaskVO;
import com.codepliot.model.GitHubIssuePageVO;
import com.codepliot.model.Result;
import com.codepliot.service.GitHubIssueService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/projects/{projectId}/github/issues")
public class GitHubIssueController {

    private final GitHubIssueService gitHubIssueService;

    public GitHubIssueController(GitHubIssueService gitHubIssueService) {
        this.gitHubIssueService = gitHubIssueService;
    }

    @GetMapping
    public Result<GitHubIssuePageVO> list(@PathVariable Long projectId,
                                          @RequestParam(defaultValue = "open") String state,
                                          @RequestParam(defaultValue = "1") Integer page,
                                          @RequestParam(defaultValue = "20") Integer pageSize) {
        return Result.success(gitHubIssueService.listIssues(projectId, state, page, pageSize));
    }

    @PostMapping("/{issueNumber}/import-task")
    public Result<AgentTaskVO> importTask(@PathVariable Long projectId, @PathVariable Integer issueNumber) {
        return Result.success("GitHub issue imported as agent task", gitHubIssueService.importIssueAsTask(projectId, issueNumber));
    }
}
