package com.codepliot.service.git;

import com.codepliot.config.GitOperationProperties;
import com.codepliot.entity.ProjectRepo;
import com.codepliot.exception.BusinessException;
import com.codepliot.model.ErrorCode;
import com.codepliot.repository.ProjectRepoMapper;
import com.codepliot.service.auth.GitHubAuthService;
import java.nio.file.Files;
import java.nio.file.Path;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.ResetCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Repository;
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
            syncRepository(projectRepo, repositoryPath);
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

    public String syncRepository(Long projectId) {
        ProjectRepo projectRepo = requireProjectRepo(projectId);
        Path repositoryPath = gitWorkspaceService.getRepositoryPath(projectId);
        if (!gitWorkspaceService.hasGitDirectory(projectId)) {
            return cloneRepository(projectId);
        }
        syncRepository(projectRepo, repositoryPath);
        return updateLocalPath(projectRepo, repositoryPath);
    }

    private void syncRepository(ProjectRepo projectRepo, Path repositoryPath) {
        try (Git git = Git.open(repositoryPath.toFile())) {
            String baseBranch = resolveBaseBranch(projectRepo, git.getRepository());
            fetchOrigin(git, projectRepo.getUserId());
            checkoutBaseBranch(git, git.getRepository(), baseBranch);
            git.reset().setMode(ResetCommand.ResetType.HARD).setRef("origin/" + baseBranch).call();
            git.clean().setCleanDirectories(true).setIgnore(false).call();
        } catch (Exception ex) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR,
                    "Failed to sync GitHub repository: " + buildErrorMessage(ex));
        }
    }

    private void fetchOrigin(Git git, Long userId) throws GitAPIException {
        var fetchCommand = git.fetch().setRemote("origin");
        String accessToken = gitHubAuthService.resolveAccessTokenForUser(userId);
        if (accessToken != null && !accessToken.isBlank()) {
            fetchCommand.setCredentialsProvider(new UsernamePasswordCredentialsProvider(accessToken.trim(), "x-oauth-basic"));
        }
        fetchCommand.call();
    }

    private void checkoutBaseBranch(Git git, Repository repository, String baseBranch) throws Exception {
        if (repository.findRef(baseBranch) == null) {
            git.checkout()
                    .setCreateBranch(true)
                    .setName(baseBranch)
                    .setStartPoint("origin/" + baseBranch)
                    .call();
        } else {
            git.checkout().setName(baseBranch).call();
        }
    }

    private String resolveBaseBranch(ProjectRepo projectRepo, Repository repository) {
        if (projectRepo.getDefaultBranch() != null && !projectRepo.getDefaultBranch().isBlank()) {
            return projectRepo.getDefaultBranch().trim();
        }
        try {
            String branch = repository.getBranch();
            return branch == null || branch.isBlank() ? "main" : branch;
        } catch (Exception exception) {
            return "main";
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
