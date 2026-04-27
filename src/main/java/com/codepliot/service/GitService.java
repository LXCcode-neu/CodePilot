package com.codepliot.service;

import com.codepliot.exception.BusinessException;
import com.codepliot.model.ErrorCode;
import com.codepliot.entity.ProjectRepo;
import com.codepliot.repository.ProjectRepoMapper;
import java.nio.file.Files;
import java.nio.file.Path;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.springframework.stereotype.Service;
/**
 * GitService 服务类，负责封装业务流程和领域能力。
 */
@Service
public class GitService {

    private final ProjectRepoMapper projectRepoMapper;
    private final GitWorkspaceService gitWorkspaceService;
/**
 * 创建 GitService 实例。
 */
public GitService(ProjectRepoMapper projectRepoMapper, GitWorkspaceService gitWorkspaceService) {
        this.projectRepoMapper = projectRepoMapper;
        this.gitWorkspaceService = gitWorkspaceService;
    }
/**
 * 克隆Repository相关逻辑。
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
 * 检查并返回Project Repo相关逻辑。
 */
private ProjectRepo requireProjectRepo(Long projectId) {
        ProjectRepo projectRepo = projectRepoMapper.selectById(projectId);
        if (projectRepo == null) {
            throw new BusinessException(ErrorCode.PROJECT_REPO_NOT_FOUND);
        }
        return projectRepo;
    }
/**
 * 更新Local Path相关逻辑。
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
 * 构建Error Message相关逻辑。
 */
private String buildErrorMessage(Exception ex) {
        String message = ex.getMessage();
        if (message == null || message.isBlank()) {
            return ex.getClass().getSimpleName();
        }
        return message;
    }
}

