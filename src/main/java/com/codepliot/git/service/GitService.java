package com.codepliot.git.service;

import com.codepliot.common.exception.BusinessException;
import com.codepliot.common.result.ErrorCode;
import com.codepliot.project.entity.ProjectRepo;
import com.codepliot.project.mapper.ProjectRepoMapper;
import java.nio.file.Files;
import java.nio.file.Path;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.springframework.stereotype.Service;

/**
 * Git 领域服务。
 * 当前只负责公开 GitHub 仓库的 clone，以及 clone 成功后的本地路径回写。
 */
@Service
public class GitService {

    private final ProjectRepoMapper projectRepoMapper;
    private final GitWorkspaceService gitWorkspaceService;

    public GitService(ProjectRepoMapper projectRepoMapper, GitWorkspaceService gitWorkspaceService) {
        this.projectRepoMapper = projectRepoMapper;
        this.gitWorkspaceService = gitWorkspaceService;
    }

    /**
     * 根据项目 ID 拉取仓库。
     * 如果目标目录已存在且包含 .git，则直接复用并回写 localPath。
     */
    public String cloneRepository(Long projectId) {
        ProjectRepo projectRepo = requireProjectRepo(projectId);
        Path repositoryPath = gitWorkspaceService.getRepositoryPath(projectId);

        if (gitWorkspaceService.hasGitDirectory(projectId)) {
            return updateLocalPath(projectRepo, repositoryPath);
        }

        if (Files.exists(repositoryPath)) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR,
                    "Repository directory already exists but is not a Git repository: " + repositoryPath);
        }

        gitWorkspaceService.ensureProjectWorkspace(projectId);
        try (Git ignored = Git.cloneRepository()
                .setURI(projectRepo.getRepoUrl())
                .setDirectory(repositoryPath.toFile())
                .call()) {
            return updateLocalPath(projectRepo, repositoryPath);
        } catch (GitAPIException | RuntimeException ex) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR,
                    "Failed to clone public GitHub repository: " + buildErrorMessage(ex));
        }
    }

    /**
     * 查询项目仓库记录，不存在则抛出业务异常。
     */
    private ProjectRepo requireProjectRepo(Long projectId) {
        ProjectRepo projectRepo = projectRepoMapper.selectById(projectId);
        if (projectRepo == null) {
            throw new BusinessException(ErrorCode.PROJECT_REPO_NOT_FOUND);
        }
        return projectRepo;
    }

    /**
     * 将 clone 后的本地路径写回 project_repo.local_path。
     */
    private String updateLocalPath(ProjectRepo projectRepo, Path repositoryPath) {
        String localPath = repositoryPath.toString();
        projectRepo.setLocalPath(localPath);
        int updated = projectRepoMapper.updateById(projectRepo);
        if (updated != 1) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "Failed to update project repository localPath");
        }
        return localPath;
    }

    /**
     * 尽量提取更可读的异常消息，便于统一透传给上层。
     */
    private String buildErrorMessage(Exception ex) {
        String message = ex.getMessage();
        if (message == null || message.isBlank()) {
            return ex.getClass().getSimpleName();
        }
        return message;
    }
}
