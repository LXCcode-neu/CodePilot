package com.codepliot.controller;

import com.codepliot.model.GitHubIssueEventRunResult;
import com.codepliot.model.GitHubIssueEventVO;
import com.codepliot.model.Result;
import com.codepliot.service.GitHubIssueEventService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/issues/events")
public class GitHubIssueEventController {

    private final GitHubIssueEventService gitHubIssueEventService;

    public GitHubIssueEventController(GitHubIssueEventService gitHubIssueEventService) {
        this.gitHubIssueEventService = gitHubIssueEventService;
    }

    @GetMapping
    public Result<List<GitHubIssueEventVO>> list(@RequestParam(required = false) String status,
                                                 @RequestParam(defaultValue = "1") Integer page,
                                                 @RequestParam(defaultValue = "20") Integer pageSize) {
        return Result.success(gitHubIssueEventService.listCurrentUserEvents(status, page, pageSize));
    }

    @PostMapping("/{id}/ignore")
    public Result<GitHubIssueEventVO> ignore(@PathVariable Long id) {
        return Result.success("GitHub issue event ignored", gitHubIssueEventService.ignore(id));
    }

    @PostMapping("/{id}/run")
    public Result<GitHubIssueEventRunResult> run(@PathVariable Long id) {
        return Result.success("GitHub issue repair started", gitHubIssueEventService.run(id));
    }
}
