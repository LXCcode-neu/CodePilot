package com.codepliot.service.githubIssue;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.codepliot.client.GitHubIssueClient;
import com.codepliot.entity.ProjectRepo;
import com.codepliot.exception.BusinessException;
import com.codepliot.model.AgentTaskCreateRequest;
import com.codepliot.model.AgentTaskVO;
import com.codepliot.model.ErrorCode;
import com.codepliot.model.GitHubIssuePageVO;
import com.codepliot.model.GitHubIssueVO;
import com.codepliot.repository.ProjectRepoMapper;
import com.codepliot.service.auth.GitHubAuthService;
import com.codepliot.service.task.AgentTaskService;
import com.codepliot.utils.SecurityUtils;
import org.springframework.stereotype.Service;

@Service
public class GitHubIssueService {

    private final ProjectRepoMapper projectRepoMapper;
    private final GitHubIssueClient gitHubIssueClient;
    private final AgentTaskService agentTaskService;
    private final GitHubAuthService gitHubAuthService;

    public GitHubIssueService(ProjectRepoMapper projectRepoMapper,
                              GitHubIssueClient gitHubIssueClient,
                              AgentTaskService agentTaskService,
                              GitHubAuthService gitHubAuthService) {
        this.projectRepoMapper = projectRepoMapper;
        this.gitHubIssueClient = gitHubIssueClient;
        this.agentTaskService = agentTaskService;
        this.gitHubAuthService = gitHubAuthService;
    }

    public GitHubIssuePageVO listIssues(Long projectId, String state, Integer page, Integer pageSize) {
        ProjectRepo projectRepo = requireOwnedRepo(projectId);
        GitHubRepoRef repoRef = parseRepoRef(projectRepo.getRepoUrl());
        return gitHubIssueClient.listIssues(
                gitHubAuthService.resolveAccessTokenForCurrentUser(),
                repoRef.owner(),
                repoRef.repo(),
                state,
                page == null ? 1 : page,
                pageSize == null ? 10 : pageSize
        );
    }

    public AgentTaskVO importIssueAsTask(Long projectId, Integer issueNumber) {
        if (issueNumber == null || issueNumber <= 0) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "issueNumber must be positive");
        }

        ProjectRepo projectRepo = requireOwnedRepo(projectId);
        GitHubRepoRef repoRef = parseRepoRef(projectRepo.getRepoUrl());
        GitHubIssueVO issue = gitHubIssueClient.getIssue(
                gitHubAuthService.resolveAccessTokenForCurrentUser(),
                repoRef.owner(),
                repoRef.repo(),
                issueNumber
        );
        String description = buildIssueDescription(issue);
        return agentTaskService.create(new AgentTaskCreateRequest(projectRepo.getId(), issue.title(), description));
    }

    private ProjectRepo requireOwnedRepo(Long projectId) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        ProjectRepo projectRepo = projectRepoMapper.selectOne(new LambdaQueryWrapper<ProjectRepo>()
                .eq(ProjectRepo::getId, projectId)
                .eq(ProjectRepo::getUserId, currentUserId)
                .last("limit 1"));
        if (projectRepo == null) {
            throw new BusinessException(ErrorCode.PROJECT_REPO_NOT_FOUND);
        }
        return projectRepo;
    }

    private GitHubRepoRef parseRepoRef(String repoUrl) {
        String normalized = repoUrl == null ? "" : repoUrl.trim();
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        if (normalized.endsWith(".git")) {
            normalized = normalized.substring(0, normalized.length() - 4);
        }

        String prefix = "https://github.com/";
        if (!normalized.startsWith(prefix)) {
            throw new BusinessException(ErrorCode.INVALID_GITHUB_REPO_URL);
        }
        String[] parts = normalized.substring(prefix.length()).split("/");
        if (parts.length != 2 || parts[0].isBlank() || parts[1].isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_GITHUB_REPO_URL);
        }
        return new GitHubRepoRef(parts[0], parts[1]);
    }

    private String buildIssueDescription(GitHubIssueVO issue) {
        String body = issue.body() == null || issue.body().isBlank() ? "(No issue body)" : issue.body().trim();
        return body + "\n\nGitHub Issue: " + issue.htmlUrl();
    }

    private record GitHubRepoRef(String owner, String repo) {
    }
}
