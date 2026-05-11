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
import com.codepliot.service.GitHubAuthService;
import com.codepliot.service.ProjectLlmConfigService;
import com.codepliot.service.task.AgentTaskService;
import com.codepliot.utils.SecurityUtils;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProjectRepoService {

    private final ProjectRepoMapper projectRepoMapper;
    private final AgentTaskService agentTaskService;
    private final ProjectLlmConfigService projectLlmConfigService;
    private final GitHubAuthService gitHubAuthService;

    public ProjectRepoService(ProjectRepoMapper projectRepoMapper,
                              AgentTaskService agentTaskService,
                              ProjectLlmConfigService projectLlmConfigService,
                              GitHubAuthService gitHubAuthService) {
        this.projectRepoMapper = projectRepoMapper;
        this.agentTaskService = agentTaskService;
        this.projectLlmConfigService = projectLlmConfigService;
        this.gitHubAuthService = gitHubAuthService;
    }

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

    @Transactional
    public void delete(Long id) {
        ProjectRepo projectRepo = requireOwnedRepo(id);
        agentTaskService.deleteByProjectId(projectRepo.getId());
        projectLlmConfigService.deleteByProjectId(projectRepo.getId());
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
