package com.codepliot.service.project;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.codepliot.entity.ProjectRepo;
import com.codepliot.entity.ProjectRepoStatus;
import com.codepliot.exception.BusinessException;
import com.codepliot.model.GitHubAuthorizedRepoVO;
import com.codepliot.model.ErrorCode;
import com.codepliot.model.GitHubRepoImportRequest;
import com.codepliot.model.ProjectCreateRequest;
import com.codepliot.model.ProjectRepoVO;
import com.codepliot.repository.ProjectRepoMapper;
import com.codepliot.service.auth.GitHubAuthService;
import com.codepliot.service.llm.ProjectLlmConfigService;
import com.codepliot.service.sentry.SentryProjectMappingService;
import com.codepliot.service.task.AgentTaskService;
import com.codepliot.utils.SecurityUtils;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 项目仓库服务。
 * <p>
 * 管理用户关联的 GitHub 项目仓库，包括：
 * <ul>
 *   <li>通过手动输入 URL 创建项目仓库</li>
 *   <li>通过 GitHub OAuth 授权导入仓库</li>
 *   <li>查询当前用户的项目仓库列表和详情</li>
 *   <li>删除项目仓库（级联删除关联的 Agent 任务、LLM 配置和 Sentry 映射）</li>
 * </ul>
 */
@Service
public class ProjectRepoService {

    private final ProjectRepoMapper projectRepoMapper;
    private final AgentTaskService agentTaskService;
    private final ProjectLlmConfigService projectLlmConfigService;
    private final SentryProjectMappingService sentryProjectMappingService;
    private final GitHubAuthService gitHubAuthService;

    public ProjectRepoService(ProjectRepoMapper projectRepoMapper,
                              AgentTaskService agentTaskService,
                              ProjectLlmConfigService projectLlmConfigService,
                              SentryProjectMappingService sentryProjectMappingService,
                              GitHubAuthService gitHubAuthService) {
        this.projectRepoMapper = projectRepoMapper;
        this.agentTaskService = agentTaskService;
        this.projectLlmConfigService = projectLlmConfigService;
        this.sentryProjectMappingService = sentryProjectMappingService;
        this.gitHubAuthService = gitHubAuthService;
    }

    /**
     * 通过手动输入 GitHub 仓库 URL 创建项目仓库。
     *
     * @param request 创建请求，包含仓库 URL
     * @return 创建的项目仓库视图对象
     */
    @Transactional
    public ProjectRepoVO create(ProjectCreateRequest request) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        String normalizedRepoUrl = normalizeRepoUrl(request.repoUrl());
        String repoName = parseRepoName(normalizedRepoUrl);

        ProjectRepo projectRepo = new ProjectRepo();
        projectRepo.setUserId(currentUserId);
        projectRepo.setRepoUrl(normalizedRepoUrl);
        projectRepo.setRepoName(repoName);
        projectRepo.setGithubOwner(parseRepoOwner(normalizedRepoUrl));
        projectRepo.setGithubRepoName(repoName);
        projectRepo.setGithubRepoId(null);
        projectRepo.setGithubPrivate(false);
        projectRepo.setLocalPath(null);
        projectRepo.setDefaultBranch(null);
        projectRepo.setStatus(ProjectRepoStatus.CREATED.name());
        projectRepoMapper.insert(projectRepo);
        return ProjectRepoVO.from(projectRepo);
    }

    /**
     * 通过 GitHub OAuth 授权导入仓库。
     * <p>
     * 验证用户对该仓库的访问权限，并检查是否已存在重复导入。
     *
     * @param request 导入请求，包含仓库所有者、名称和 GitHub 仓库 ID
     * @return 创建的项目仓库视图对象
     */
    @Transactional
    public ProjectRepoVO importFromGitHub(GitHubRepoImportRequest request) {
        GitHubAuthorizedRepoVO repository = gitHubAuthService.requireAuthorizedRepository(
                request.owner().trim(),
                request.repoName().trim()
        );
        if (repository.id() == null || !repository.id().equals(request.githubRepoId())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "GitHub repository validation failed");
        }
        Long currentUserId = SecurityUtils.getCurrentUserId();
        String repoUrl = normalizeRepoUrl(repository.cloneUrl());
        ensureNotDuplicated(currentUserId, repository.id(), repoUrl);

        ProjectRepo projectRepo = new ProjectRepo();
        projectRepo.setUserId(currentUserId);
        projectRepo.setRepoUrl(repoUrl);
        projectRepo.setRepoName(repository.name());
        projectRepo.setGithubOwner(repository.owner());
        projectRepo.setGithubRepoName(repository.name());
        projectRepo.setGithubRepoId(repository.id());
        projectRepo.setGithubPrivate(repository.privateRepo());
        projectRepo.setLocalPath(null);
        projectRepo.setDefaultBranch(repository.defaultBranch());
        projectRepo.setStatus(ProjectRepoStatus.CREATED.name());
        projectRepoMapper.insert(projectRepo);
        return ProjectRepoVO.from(projectRepo);
    }

    /**
     * 查询当前用户的所有项目仓库列表，按创建时间倒序排列。
     *
     * @return 项目仓库列表
     */
    public List<ProjectRepoVO> listCurrentUserRepos() {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        return projectRepoMapper.selectList(new LambdaQueryWrapper<ProjectRepo>()
                        .eq(ProjectRepo::getUserId, currentUserId)
                        .orderByDesc(ProjectRepo::getCreatedAt))
                .stream()
                .map(ProjectRepoVO::from)
                .toList();
    }

    public ProjectRepoVO getDetail(Long id) {
        return ProjectRepoVO.from(requireOwnedRepo(id));
    }

    /**
     * 删除指定的项目仓库及其所有关联数据。
     * <p>
     * 级联删除：Agent 任务、LLM 配置、Sentry 项目映射。
     *
     * @param id 项目仓库 ID
     */
    @Transactional
    public void delete(Long id) {
        ProjectRepo projectRepo = requireOwnedRepo(id);
        agentTaskService.deleteByProjectId(projectRepo.getId());
        projectLlmConfigService.deleteByProjectId(projectRepo.getId());
        sentryProjectMappingService.deleteByProjectId(projectRepo.getId());
        projectRepoMapper.deleteById(projectRepo.getId());
    }

    private ProjectRepo requireOwnedRepo(Long id) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        ProjectRepo projectRepo = projectRepoMapper.selectOne(new LambdaQueryWrapper<ProjectRepo>()
                .eq(ProjectRepo::getId, id)
                .eq(ProjectRepo::getUserId, currentUserId)
                .last("limit 1"));
        if (projectRepo == null) {
            throw new BusinessException(ErrorCode.PROJECT_REPO_NOT_FOUND);
        }
        return projectRepo;
    }

    private String normalizeRepoUrl(String repoUrl) {
        String normalized = repoUrl == null ? "" : repoUrl.trim();
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        if (!normalized.matches("^https://github\\.com/[^/]+/[^/]+(\\.git)?$")) {
            throw new BusinessException(ErrorCode.INVALID_GITHUB_REPO_URL);
        }
        return normalized;
    }

    private String parseRepoName(String repoUrl) {
        String repoName = repoUrl.substring(repoUrl.lastIndexOf('/') + 1);
        if (repoName.endsWith(".git")) {
            repoName = repoName.substring(0, repoName.length() - 4);
        }
        if (repoName.isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_GITHUB_REPO_URL);
        }
        return repoName;
    }

    private String parseRepoOwner(String repoUrl) {
        String normalized = repoUrl;
        if (normalized.endsWith(".git")) {
            normalized = normalized.substring(0, normalized.length() - 4);
        }
        String[] parts = normalized.substring("https://github.com/".length()).split("/");
        if (parts.length != 2 || parts[0].isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_GITHUB_REPO_URL);
        }
        return parts[0];
    }

    private void ensureNotDuplicated(Long userId, Long githubRepoId, String repoUrl) {
        ProjectRepo existingByUrl = projectRepoMapper.selectOne(new LambdaQueryWrapper<ProjectRepo>()
                .eq(ProjectRepo::getUserId, userId)
                .eq(ProjectRepo::getRepoUrl, repoUrl)
                .last("limit 1"));
        if (existingByUrl != null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "This GitHub repository has already been added");
        }
        if (githubRepoId == null) {
            return;
        }
        ProjectRepo existingByRepoId = projectRepoMapper.selectOne(new LambdaQueryWrapper<ProjectRepo>()
                .eq(ProjectRepo::getUserId, userId)
                .eq(ProjectRepo::getGithubRepoId, githubRepoId)
                .last("limit 1"));
        if (existingByRepoId != null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "This GitHub repository has already been added");
        }
    }
}
