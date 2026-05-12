package com.codepliot.service.git;

import com.codepliot.config.GitOperationProperties;
import com.codepliot.entity.ProjectRepo;
import com.codepliot.exception.BusinessException;
import com.codepliot.model.ErrorCode;
import com.codepliot.repository.ProjectRepoMapper;
import com.codepliot.service.GitHubAuthService;
import java.nio.file.Files;
import java.nio.file.Path;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.springframework.stereotype.Service;

@Service
public class GitService {

    private final ProjectRepoMapper projectRepoMapper;
    private final GitWorkspaceService gitWorkspaceService;
    private final GitHubAuthService gitHubAuthService;
    private final GitOperationProperties gitOperationProperties;

    public GitService(ProjectRepoMapper projectRepoMapper,
                      GitWorkspaceService gitWorkspaceService,
                      GitHubAuthService gitHubAuthService,
                      GitOperationProperties gitOperationProperties) {
        this.projectRepoMapper = projectRepoMapper;
        this.gitWorkspaceService = gitWorkspaceService;
        this.gitHubAuthService = gitHubAuthService;
        this.gitOperationProperties = gitOperationProperties;
    }

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
        try {
            var cloneCommand = Git.cloneRepository()
                    .setURI(projectRepo.getRepoUrl())
                    .setDirectory(repositoryPath.toFile())
                    .setTimeout(Math.max(gitOperationProperties.getCloneTimeoutSeconds(), 1));
            String accessToken = gitHubAuthService.resolveAccessTokenForUser(projectRepo.getUserId());
            if (accessToken != null && !accessToken.isBlank()) {
                cloneCommand.setCredentialsProvider(new UsernamePasswordCredentialsProvider(accessToken.trim(), "x-oauth-basic"));
            }
            try (Git ignored = cloneCommand.call()) {
                return updateLocalPath(projectRepo, repositoryPath);
            }
        } catch (GitAPIException | RuntimeException ex) {
            gitWorkspaceService.deleteRepositoryWorkspace(projectId);
            throw new BusinessException(ErrorCode.INTERNAL_ERROR,
                    "Failed to clone GitHub repository: " + buildErrorMessage(ex));
        }
    }

    private ProjectRepo requireProjectRepo(Long projectId) {
        ProjectRepo projectRepo = projectRepoMapper.selectById(projectId);
        if (projectRepo == null) {
            throw new BusinessException(ErrorCode.PROJECT_REPO_NOT_FOUND);
        }
        return projectRepo;
    }

    private String updateLocalPath(ProjectRepo projectRepo, Path repositoryPath) {
        String localPath = repositoryPath.toString();
        projectRepo.setLocalPath(localPath);
        int updated = projectRepoMapper.updateById(projectRepo);
        if (updated != 1) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "Failed to update project repository localPath");
        }
        return localPath;
    }

    private String buildErrorMessage(Exception ex) {
        String message = ex.getMessage();
        if (message == null || message.isBlank()) {
            return ex.getClass().getSimpleName();
        }
        return message;
    }
}
