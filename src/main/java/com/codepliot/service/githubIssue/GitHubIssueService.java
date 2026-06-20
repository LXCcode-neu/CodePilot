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

/**
 * GitHub Issue 服务。
 * <p>
 * 提供与 GitHub Issue 相关的业务操作，包括：
 * <ul>
 *   <li>查询指定项目的 GitHub Issue 列表</li>
 *   <li>将 GitHub Issue 导入为系统 Agent 任务</li>
 * </ul>
 * <p>
 * 通过 GitHub API 获取 Issue 数据，并支持从 Issue URL 中解析仓库所有者和名称。
 */
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

    /**
     * 分页查询指定项目的 GitHub Issue 列表。
     *
     * @param projectId 项目仓库 ID
     * @param state     Issue 状态过滤（如 open、closed）
     * @param page      页码
     * @param pageSize  每页大小
     * @return Issue 分页结果
     */
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

    /**
     * 将指定的 GitHub Issue 导入为系统 Agent 任务。
     * <p>
     * 从 GitHub API 获取 Issue 详情，以 Issue 标题和描述创建 Agent 任务。
     *
     * @param projectId   项目仓库 ID
     * @param issueNumber GitHub Issue 编号
     * @return 创建的 Agent 任务视图对象
     */
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
